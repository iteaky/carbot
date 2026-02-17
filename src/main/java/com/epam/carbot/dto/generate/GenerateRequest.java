package com.epam.carbot.dto.generate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GenerateRequest(
        String prompt,
        @JsonProperty("chat_mode") String chatMode,
        @JsonProperty("chat_url") String chatUrl
) {
}
