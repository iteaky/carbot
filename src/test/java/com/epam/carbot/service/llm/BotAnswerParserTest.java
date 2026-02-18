package com.epam.carbot.service.llm;

import com.epam.carbot.domain.BotAnswer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BotAnswerParserTest {

    private final BotAnswerParser parser = new BotAnswerParser();

    @Test
    void parsesPlainJson() throws Exception {
        String payload = """
                {
                  "reply": "ok",
                  "memory": {
                    "budget": "1000",
                    "country": "Китай",
                    "purpose": "город",
                    "body_type": "хэтчбек",
                    "summary": "s"
                  }
                }
                """;

        BotAnswer answer = parser.parse(payload);

        assertEquals("ok", answer.reply());
        assertEquals("1000", answer.memory().budget());
    }

    @Test
    void parsesJsonInsideMarkdownFence() throws Exception {
        String payload = """
                ```json
                {
                  "reply": "ok",
                  "memory": {
                    "budget": "1000",
                    "country": "Китай",
                    "purpose": "город",
                    "body_type": "хэтчбек",
                    "summary": "s"
                  }
                }
                ```
                """;

        BotAnswer answer = parser.parse(payload);

        assertEquals("ok", answer.reply());
    }

    @Test
    void parsesJsonWithJsonTokenPrefix() throws Exception {
        String payload = """
                json
                {
                  "reply": "ok",
                  "memory": {
                    "budget": "1000",
                    "country": "Китай",
                    "purpose": "город",
                    "body_type": "хэтчбек",
                    "summary": "s"
                  }
                }
                """;

        BotAnswer answer = parser.parse(payload);

        assertEquals("ok", answer.reply());
    }

    @Test
    void parsesJsonWithRawNewlineInsideReplyString() throws Exception {
        String payload = """
                {
                  "reply": "строка 1
                строка 2",
                  "memory": {
                    "budget": "1000",
                    "country": "Китай",
                    "purpose": "город",
                    "body_type": "хэтчбек",
                    "summary": "s"
                  }
                }
                """;

        BotAnswer answer = parser.parse(payload);

        assertEquals("строка 1\nстрока 2", answer.reply());
    }
}
