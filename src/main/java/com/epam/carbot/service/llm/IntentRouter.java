package com.epam.carbot.service.llm;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class IntentRouter {

    public Intent detectIntent(String message, List<String> missingFields, String pendingField) {
        if (message == null || message.isBlank()) {
            return Intent.OTHER;
        }

        String text = message.trim();
        String lower = text.toLowerCase(Locale.ROOT);

        if (looksLikeClarificationQuestion(text, lower)) {
            return Intent.ASK_CLARIFICATION;
        }

        if (pendingField != null && !pendingField.isBlank() && looksLikeFieldValue(lower, pendingField)) {
            return Intent.PROVIDE_INFO;
        }

        if (missingFields != null && !missingFields.isEmpty()) {
            return Intent.PROVIDE_INFO;
        }

        return Intent.OTHER;
    }

    private boolean looksLikeClarificationQuestion(String text, String lower) {
        return text.contains("?")
                || lower.contains("что лучше")
                || lower.contains("почему")
                || lower.contains("в чем разница")
                || lower.contains("а если")
                || lower.contains("как выбрать")
                || lower.contains("какой лучше");
    }

    private boolean looksLikeFieldValue(String lower, String pendingField) {
        return switch (pendingField) {
            case "budget" -> lower.matches(".*\\d.*")
                    || lower.contains("руб")
                    || lower.contains("usd")
                    || lower.contains("eur");
            case "country" -> !lower.contains("?") && lower.length() <= 40;
            case "purpose", "body_type" -> !lower.contains("?");
            default -> true;
        };
    }

    public enum Intent {
        PROVIDE_INFO,
        ASK_CLARIFICATION,
        OTHER
    }
}
