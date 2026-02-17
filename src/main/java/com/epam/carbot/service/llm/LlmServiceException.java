package com.epam.carbot.service.llm;

public class LlmServiceException extends RuntimeException {
    public LlmServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
