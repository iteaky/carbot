package com.epam.carbot.service.llm;

import com.epam.carbot.domain.Memory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
public class PromptBuilder {

    private final ObjectMapper mapper = new ObjectMapper();

    public String build(String systemPrompt, Memory memory, List<String> missingFields, String message) {
        String memoryJson;
        String missingJson;
        try {
            memoryJson = mapper.writeValueAsString(memory);
            missingJson = mapper.writeValueAsString(missingFields);
        } catch (Exception e) {
            memoryJson = "null";
            missingJson = "[]";
        }

        return """
                SYSTEM:
                %s

                Контекст (READ-ONLY):
                memory=%s
                missingFields=%s

                Сообщение пользователя:
                %s
                """.formatted(systemPrompt, memoryJson, missingJson, message);
    }
}
