package com.epam.carbot.service.impl;

import lombok.Getter;

public enum ChatMode {

    NEW("new"),
    CONTINUE("continue"),
    INCOGNITO("incognito");

    @Getter
    private final String code;

    ChatMode(String code) {
        this.code = code;
    }
}
