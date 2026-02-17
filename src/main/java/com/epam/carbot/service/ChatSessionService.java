package com.epam.carbot.service;
import com.epam.carbot.dto.chat.ChatMessage;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatSessionService {

    // sessionId -> history
    private final Map<String, List<ChatMessage>> history = new ConcurrentHashMap<>();

    public String sessionId() {
        var s = VaadinSession.getCurrent();
        String id = (String) s.getAttribute("chatSessionId");
        if (id == null) {
            id = UUID.randomUUID().toString();
            s.setAttribute("chatSessionId", id);
        }
        return id;
    }

    public String username() {
        return (String) VaadinSession.getCurrent().getAttribute("username");
    }

    public void setUsername(String name) {
        VaadinSession.getCurrent().setAttribute("username", name);
    }

    public List<ChatMessage> getHistory(String sessionId) {
        return history.computeIfAbsent(sessionId, _ -> new ArrayList<>());
    }

    public void addUserMessage(String sessionId, String username, String text) {
        getHistory(sessionId).add(new ChatMessage(username, text, Instant.now(), true));
    }

    public void addBotMessage(String sessionId, String text) {
        getHistory(sessionId).add(new ChatMessage("AutoBot", text, Instant.now(), false));
    }
}
