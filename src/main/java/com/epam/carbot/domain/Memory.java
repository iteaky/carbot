package com.epam.carbot.domain;

public record Memory(
        String budget,
        String country,
        String purpose,
        String body_type,
        String summary
) {
}
