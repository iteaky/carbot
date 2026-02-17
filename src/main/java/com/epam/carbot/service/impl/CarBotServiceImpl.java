package com.epam.carbot.service.impl;

import com.epam.carbot.domain.BotAnswer;
import com.epam.carbot.domain.Memory;
import com.epam.carbot.dto.generate.GenerateRequest;
import com.epam.carbot.dto.generate.GenerateResponse;
import com.epam.carbot.service.CarBotService;
import com.epam.carbot.service.llm.BotAnswerParser;
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
            1) Если поле в memory уже заполнено — НЕ переспрашивай.
            2) Не сбрасывай заполненные поля в null. Если не уверен — оставь как есть.
            3) missingFields — список полей, которые нужно добрать. Если missingFields НЕ пуст:
               - задай ОДИН вопрос только про ПЕРВОЕ поле из missingFields.
            4) Если missingFields пуст:
               - предложи 2-3 конкретные модели авто под параметры и кратко объясни почему.
            5) summary — коротко (1-2 строки) факты.

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

    public CarBotServiceImpl(
            LlmClient llmClient,
            PromptBuilder promptBuilder,
            BotAnswerParser answerParser,
            MemoryStore memoryStore,
            MemoryService memoryService
    ) {
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.answerParser = answerParser;
        this.memoryStore = memoryStore;
        this.memoryService = memoryService;
    }

    @Override
    public String reply(String username, String userText) {
        return sendRequest(username, userText);
    }

    public String sendRequest(String username, String message) {
        Memory current = memoryStore.get(username);

        List<String> missingFields = memoryService.computeMissingFields(current);
        String prompt = promptBuilder.build(SYSTEM_PROMPT, current, missingFields, message);
        GenerateRequest request = new GenerateRequest(prompt, "new", null);

        GenerateResponse body;
        try {
            body = llmClient.generate(request);
        } catch (LlmBusyException e) {
            logger.warn("llm busy: {}", e.getMessage());
            return BUSY_MESSAGE;
        } catch (LlmInvalidRequestException e) {
            logger.warn("llm invalid request: {}", e.getMessage());
            return INVALID_MESSAGE;
        } catch (LlmServiceException e) {
            logger.error("llm error", e);
            return ERROR_MESSAGE;
        }

        if (body == null || Boolean.FALSE.equals(body.ok()) || body.text() == null) {
            return UNHEARD_MESSAGE;
        }

        String content = body.text();

        try {
            BotAnswer answer = answerParser.parse(content);

            Memory merged = (current != null)
                    ? memoryService.merge(current, answer.memory())
                    : memoryService.sanitizeNewMemory(answer.memory());

            memoryStore.put(username, merged);

            String reply = answer.reply();

            logger.info("answer: {}", reply);
            logger.info("answer memory: {}", merged);

            return reply;

        } catch (Exception e) {
            logger.warn("parse error", e);
            return UNHEARD_MESSAGE;
        }
    }
}
