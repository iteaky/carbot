package com.epam.carbot.service.llm;

import com.epam.carbot.domain.BotAnswer;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class BotAnswerParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public BotAnswer parse(String content) throws Exception {
        return mapper.readValue(content, BotAnswer.class);
    }
}
