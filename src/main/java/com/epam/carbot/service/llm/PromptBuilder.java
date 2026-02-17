package com.epam.carbot.service.llm;

import com.epam.carbot.domain.Memory;
import com.epam.carbot.dto.chat.ChatMessage;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
public class PromptBuilder {

    private final ObjectMapper mapper = new ObjectMapper();

    public String build(
            String systemPrompt,
            Memory memory,
            List<String> missingFields,
            String pendingField,
            String intent,
            List<ChatMessage> recentHistory,
            String message
    ) {
        String memoryJson;
        String missingJson;
        String historyJson;
        try {
            memoryJson = mapper.writeValueAsString(memory);
            missingJson = mapper.writeValueAsString(missingFields);
            historyJson = mapper.writeValueAsString(toHistoryPayload(recentHistory));
        } catch (Exception e) {
            memoryJson = "null";
            missingJson = "[]";
            historyJson = "[]";
        }

        return """
                SYSTEM:
                %s

                Контекст (READ-ONLY):
                memory=%s
                missingFields=%s
                pendingField=%s
                intent=%s
                historyWindow=%s

                Сообщение пользователя:
                %s
                """.formatted(systemPrompt, memoryJson, missingJson, pendingField, intent, historyJson, message);
    }

    private List<Map<String, String>> toHistoryPayload(List<ChatMessage> recentHistory) {
        if (recentHistory == null || recentHistory.isEmpty()) {
            return List.of();
        }

        return recentHistory.stream()
                .map(msg -> Map.of(
                        "author", msg.author() == null ? "" : msg.author(),
                        "text", msg.text() == null ? "" : msg.text()
                ))
                .toList();
    }
}
