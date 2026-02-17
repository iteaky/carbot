package com.epam.carbot.dto.generate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GenerateResponse(
        Boolean ok,
        String text,
        @JsonProperty("chat_mode_used") String chatModeUsed,
        @JsonProperty("chat_url") String chatUrl
) {
}
