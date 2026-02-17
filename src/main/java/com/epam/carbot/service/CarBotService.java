package com.epam.carbot.service;

import com.epam.carbot.domain.BotReply;
import com.epam.carbot.dto.chat.ChatMessage;

import java.util.List;

public interface CarBotService {

    BotReply reply(String sessionId, String username, String userText, List<ChatMessage> recentHistory, String pendingField);

}
