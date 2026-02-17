package com.epam.carbot.service.memory;

import com.epam.carbot.domain.Memory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryMemoryStore implements MemoryStore {
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final Duration ttl;
    private final Clock clock;

    private final Map<String, MemoryEntry> memoryByUser = new ConcurrentHashMap<>();

    public InMemoryMemoryStore() {
        this(DEFAULT_TTL, Clock.systemUTC());
    }

    InMemoryMemoryStore(Duration ttl, Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    @Override
    public Memory get(String username) {
        MemoryEntry entry = memoryByUser.get(username);
        if (entry == null) {
            return null;
        }

        if (entry.isExpired(ttl, clock.instant())) {
            memoryByUser.remove(username, entry);
            return null;
        }

        return entry.memory();
    }

    @Override
    public void put(String username, Memory memory) {
        memoryByUser.put(username, new MemoryEntry(memory, clock.instant()));
    }

    private record MemoryEntry(Memory memory, Instant updatedAt) {
        private boolean isExpired(Duration ttl, Instant now) {
            return updatedAt.plus(ttl).isBefore(now);
        }
    }
}
