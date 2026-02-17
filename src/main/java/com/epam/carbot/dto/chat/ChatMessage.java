package com.epam.carbot.dto.chat;

import java.time.Instant;

public record ChatMessage(
        String author,
        String text,
        Instant at,
        boolean fromUser
) {}