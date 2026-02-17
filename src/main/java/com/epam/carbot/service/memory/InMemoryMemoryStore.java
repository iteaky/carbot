package com.epam.carbot.service.memory;

import com.epam.carbot.domain.Memory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryMemoryStore implements MemoryStore {
    private final Map<String, Memory> memoryByUser = new ConcurrentHashMap<>();

    @Override
    public Memory get(String username) {
        return memoryByUser.get(username);
    }

    @Override
    public void put(String username, Memory memory) {
        memoryByUser.put(username, memory);
    }
}
