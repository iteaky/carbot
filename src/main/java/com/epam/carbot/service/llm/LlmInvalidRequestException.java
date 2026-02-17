package com.epam.carbot.service.llm;

public class LlmInvalidRequestException extends RuntimeException {
    public LlmInvalidRequestException(String message) {
        super(message);
    }
}
