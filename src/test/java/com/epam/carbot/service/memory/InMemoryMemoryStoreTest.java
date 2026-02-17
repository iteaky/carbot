package com.epam.carbot.service.memory;

import com.epam.carbot.domain.Memory;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InMemoryMemoryStoreTest {

    @Test
    void storesMemorySeparatelyByKey() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        InMemoryMemoryStore store = new InMemoryMemoryStore(Duration.ofHours(24), clock);

        store.put("session-1", new Memory("10000", "Germany", null, null, ""));
        store.put("session-2", new Memory("20000", "Poland", null, null, ""));

        assertEquals("10000", store.get("session-1").budget());
        assertEquals("20000", store.get("session-2").budget());
    }

    @Test
    void expiresMemoryByTtl() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        InMemoryMemoryStore store = new InMemoryMemoryStore(Duration.ofMinutes(5), clock);

        store.put("session-1", new Memory("10000", "Germany", null, null, ""));
        clock.setInstant(Instant.parse("2026-01-01T00:06:00Z"));

        assertNull(store.get("session-1"));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
