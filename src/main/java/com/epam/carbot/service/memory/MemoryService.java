package com.epam.carbot.service.memory;

import com.epam.carbot.domain.Memory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MemoryService {

    public List<String> computeMissingFields(Memory m) {
        List<String> missing = new ArrayList<>();
        if (m == null || isBlank(m.budget())) missing.add("budget");
        if (m == null || isBlank(m.country())) missing.add("country");
        if (m == null || isBlank(m.purpose())) missing.add("purpose");
        if (m == null || isBlank(m.body_type())) missing.add("body_type");
        return missing;
    }

    public Memory merge(Memory oldM, Memory newM) {
        if (oldM == null) return sanitizeNewMemory(newM);
        if (newM == null) return oldM;

        return new Memory(
                firstNonBlank(newM.budget(), oldM.budget()),
                firstNonBlank(newM.country(), oldM.country()),
                firstNonBlank(newM.purpose(), oldM.purpose()),
                firstNonBlank(newM.body_type(), oldM.body_type()),
                firstNonBlank(newM.summary(), oldM.summary())
        );
    }

    public Memory sanitizeNewMemory(Memory m) {
        if (m == null) return new Memory(null, null, null, null, "");
        return new Memory(
                blankToNull(m.budget()),
                blankToNull(m.country()),
                blankToNull(m.purpose()),
                blankToNull(m.body_type()),
                (m.summary() == null ? "" : m.summary().trim())
        );
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private static String firstNonBlank(String preferred, String fallback) {
        String p = blankToNull(preferred);
        return p != null ? p : blankToNull(fallback);
    }
}
