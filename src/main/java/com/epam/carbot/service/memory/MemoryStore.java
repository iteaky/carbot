package com.epam.carbot.service.memory;

import com.epam.carbot.domain.Memory;

public interface MemoryStore {
    Memory get(String username);

    void put(String username, Memory memory);
}
