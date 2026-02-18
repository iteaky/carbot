package com.epam.carbot.service.impl;

import com.epam.carbot.domain.BotAnswer;
import com.epam.carbot.domain.BotReply;
import com.epam.carbot.domain.Memory;
import com.epam.carbot.dto.chat.ChatMessage;
import com.epam.carbot.dto.generate.GenerateRequest;
import com.epam.carbot.dto.generate.GenerateResponse;
import com.epam.carbot.service.CarBotService;
import com.epam.carbot.service.llm.BotAnswerParser;
import com.epam.carbot.service.llm.IntentRouter;
import com.epam.carbot.service.llm.LlmBusyException;
import com.epam.carbot.service.llm.LlmClient;
import com.epam.carbot.service.llm.LlmInvalidRequestException;
import com.epam.carbot.service.llm.LlmServiceException;
import com.epam.carbot.service.llm.PromptBuilder;
import com.epam.carbot.service.memory.MemoryService;
import com.epam.carbot.service.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.epam.carbot.service.impl.ChatMode.INCOGNITO;

@Service
@Primary
public class CarBotServiceImpl implements CarBotService {

    private static final String SYSTEM_PROMPT = """
            Ты автомобильный консультант по ПОКУПКЕ авто. Возвращай ТОЛЬКО один JSON-объект.
            
            ЗАДАЧА:
            - Веди разговор проактивно: сам направляй пользователя к покупке.
            - Память обязательна: используй memory как источник истины.
            - Не усложняй: задавай короткие, понятные вопросы.
            
             ПРАВИЛА:
             0) Всегда отвечай в поле reply.
             0.1) Выводи только JSON-объект, без markdown, без ```json и без пояснений вне JSON.
             1) Если поле в memory уже заполнено — НЕ переспрашивай.
            2) Не сбрасывай заполненные поля в null. Если не уверен — оставь как есть.
            3) missingFields — список полей, которые нужно добрать. Если missingFields НЕ пуст:
               - задай ОДИН вопрос только про ПЕРВОЕ поле из missingFields.
            4) Если missingFields пуст:
               - предложи 2-3 конкретные модели авто под параметры и кратко объясни почему.
             5) summary — коротко (1-2 строки) факты.
             6) Если intent=ASK_CLARIFICATION_AND_RETURN_TO_FIELD и missingFields НЕ пуст:
                - сначала ответь кратко на вопрос пользователя (1-2 предложения),
                - затем задай один вопрос только про pendingField,
                - memory не меняй по смыслу.
            
             ФОРМАТ (строго JSON):
            {
              "reply": "string",
              "memory": {
                "budget": string|null,
                "country": string|null,
                "purpose": string|null,
                "body_type": string|null,
                "summary": string
              }
            }
            """;

    private static final String BUSY_MESSAGE = "Сейчас сервис занят. Попробуйте через минуту.";
    private static final String INVALID_MESSAGE = "Не удалось обработать запрос. Переформулируйте сообщение.";
    private static final String ERROR_MESSAGE = "Сервис временно недоступен. Попробуйте позже.";
    private static final String UNHEARD_MESSAGE = "Не расслышал, повторите.";

    private static final Logger logger = LoggerFactory.getLogger(CarBotServiceImpl.class);

    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final BotAnswerParser answerParser;
    private final MemoryStore memoryStore;
    private final MemoryService memoryService;
    private final IntentRouter intentRouter;

    public CarBotServiceImpl(
            LlmClient llmClient,
            PromptBuilder promptBuilder,
            BotAnswerParser answerParser,
            MemoryStore memoryStore,
            MemoryService memoryService,
            IntentRouter intentRouter
    ) {
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.answerParser = answerParser;
        this.memoryStore = memoryStore;
        this.memoryService = memoryService;
        this.intentRouter = intentRouter;
    }

    @Override
    public BotReply reply(String sessionId, String username, String userText, List<ChatMessage> recentHistory, String pendingField) {
        return sendRequest(sessionId, username, userText, recentHistory, pendingField);
    }

    public BotReply sendRequest(
            String sessionId,
            String username,
            String message,
            List<ChatMessage> recentHistory,
            String pendingField
    ) {
        logger.debug("processing message for user={}, session={}", username, sessionId);
        Memory current = memoryStore.get(sessionId);

        List<String> missingFields = memoryService.computeMissingFields(current);
        String expectedField = missingFields.isEmpty() ? null : missingFields.get(0);

        IntentRouter.Intent intent = intentRouter.detectIntent(message, missingFields, pendingField);

        if (expectedField != null && intent == IntentRouter.Intent.OTHER) {
            return new BotReply(fieldQuestion(expectedField), expectedField);
        }

        String flowIntent = intent.name();
        if (expectedField != null && intent == IntentRouter.Intent.ASK_CLARIFICATION) {
            flowIntent = "ASK_CLARIFICATION_AND_RETURN_TO_FIELD";
        }

        String prompt = promptBuilder.build(
                SYSTEM_PROMPT,
                current,
                missingFields,
                expectedField,
                flowIntent,
                recentHistory,
                message
        );
        GenerateRequest request = new GenerateRequest(prompt, INCOGNITO.getCode(), null);

        GenerateResponse body;
        try {
            body = llmClient.generate(request);
        } catch (LlmBusyException e) {
            logger.warn("llm busy: {}", e.getMessage());
            return new BotReply(BUSY_MESSAGE, expectedField);
        } catch (LlmInvalidRequestException e) {
            logger.warn("llm invalid request: {}", e.getMessage());
            return new BotReply(INVALID_MESSAGE, expectedField);
        } catch (LlmServiceException e) {
            logger.error("llm error", e);
            return new BotReply(ERROR_MESSAGE, expectedField);
        }

        if (body == null || Boolean.FALSE.equals(body.ok()) || body.text() == null) {
            return new BotReply(UNHEARD_MESSAGE, expectedField);
        }

        String content = body.text();

        try {
            BotAnswer answer = answerParser.parse(content);

            if (expectedField != null && intent == IntentRouter.Intent.ASK_CLARIFICATION) {
                return new BotReply(answer.reply(), expectedField);
            }

            Memory merged = (current != null)
                    ? memoryService.merge(current, answer.memory())
                    : memoryService.sanitizeNewMemory(answer.memory());

            memoryStore.put(sessionId, merged);

            List<String> nextMissingFields = memoryService.computeMissingFields(merged);
            String nextPendingField = nextMissingFields.isEmpty() ? null : nextMissingFields.get(0);

            String reply = answer.reply();

            logger.info("answer: {}", reply);
            logger.info("answer memory: {}", merged);

            return new BotReply(reply, nextPendingField);

        } catch (Exception e) {
            logger.warn("parse error", e);
            return new BotReply(UNHEARD_MESSAGE, expectedField);
        }
    }

    private String fieldQuestion(String field) {
        return switch (field) {
            case "budget" -> "Чтобы подобрать варианты, подскажите ваш бюджет?";
            case "country" -> "В какой стране или городе планируете покупать авто?";
            case "purpose" -> "Для каких задач нужен автомобиль: город, трасса, семья, работа?";
            case "body_type" -> "Какой тип кузова рассматриваете: седан, кроссовер, универсал?";
            default -> "Уточните, пожалуйста, недостающие параметры для подбора.";
        };
    }
}
