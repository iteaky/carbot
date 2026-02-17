package com.epam.carbot.service.memory;

import com.epam.carbot.domain.Memory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryServiceTest {

    private final MemoryService memoryService = new MemoryService();

    @Test
    void mergeKeepsPreviousValuesAndNormalizesIncoming() {
        Memory oldMemory = new Memory("10000 usd", "Germany", "city", "sedan", "old summary");
        Memory incoming = new Memory("   ", "rUsSiA", null, " Crossover ", "new summary");

        Memory merged = memoryService.merge(oldMemory, incoming);

        assertEquals("10000 usd", merged.budget());
        assertEquals("Russia", merged.country());
        assertEquals("city", merged.purpose());
        assertEquals("crossover", merged.body_type());
        assertEquals("new summary", merged.summary());
    }
}
