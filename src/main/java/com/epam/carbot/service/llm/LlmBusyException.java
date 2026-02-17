package com.epam.carbot.service.llm;

public class LlmBusyException extends RuntimeException {
    public LlmBusyException(String message) {
        super(message);
    }
}
