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
    private final Map<String, DialogState> dialogState = new ConcurrentHashMap<>();

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
        return history.computeIfAbsent(sessionId, key -> new ArrayList<>());
    }

    public List<ChatMessage> getRecentHistory(String sessionId, int maxMessages) {
        List<ChatMessage> all = getHistory(sessionId);
        if (all.isEmpty()) {
            return List.of();
        }

        int safeLimit = Math.max(1, maxMessages);
        int fromIndex = Math.max(0, all.size() - safeLimit);
        return new ArrayList<>(all.subList(fromIndex, all.size()));
    }

    public String pendingField(String sessionId) {
        return dialogState.computeIfAbsent(sessionId, key -> new DialogState(null, null)).pendingField();
    }

    public void setPendingField(String sessionId, String pendingField) {
        dialogState.compute(sessionId, (key, existing) -> {
            DialogState current = existing == null ? new DialogState(null, null) : existing;
            return new DialogState(pendingField, current.lastBotAction());
        });
    }

    public void setLastBotAction(String sessionId, String action) {
        dialogState.compute(sessionId, (key, existing) -> {
            DialogState current = existing == null ? new DialogState(null, null) : existing;
            return new DialogState(current.pendingField(), action);
        });
    }

    public void addUserMessage(String sessionId, String username, String text) {
        getHistory(sessionId).add(new ChatMessage(username, text, Instant.now(), true));
    }

    public void addBotMessage(String sessionId, String text) {
        getHistory(sessionId).add(new ChatMessage("AutoBot", text, Instant.now(), false));
    }

    private record DialogState(String pendingField, String lastBotAction) {
    }
}
