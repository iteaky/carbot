package com.epam.carbot.service.llm;

import com.epam.carbot.domain.BotAnswer;
import org.springframework.stereotype.Component;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.ObjectMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BotAnswerParser {

    private static final Pattern FENCED_JSON = Pattern.compile("(?s)```(?:json)?\\s*(.*?)\\s*```");
    private final ObjectMapper mapper = new ObjectMapper();

    public BotAnswer parse(String content) throws Exception {
        String normalized = normalizeContent(content);

        try {
            return mapper.readValue(normalized, BotAnswer.class);
        } catch (StreamReadException e) {
            String repaired = escapeControlCharsInStrings(normalized);
            if (repaired.equals(normalized)) {
                throw e;
            }
            return mapper.readValue(repaired, BotAnswer.class);
        }
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return "";
        }

        String normalized = content.trim();
        Matcher fenced = FENCED_JSON.matcher(normalized);
        if (fenced.find()) {
            normalized = fenced.group(1).trim();
        }

        normalized = stripJsonPrefix(normalized);
        return extractFirstJsonObject(normalized);
    }

    private String stripJsonPrefix(String value) {
        String normalized = value.trim();
        if (!normalized.regionMatches(true, 0, "json", 0, 4)) {
            return normalized;
        }

        int index = 4;
        while (index < normalized.length()) {
            char ch = normalized.charAt(index);
            if (Character.isWhitespace(ch) || ch == ':' || ch == '-') {
                index++;
                continue;
            }
            break;
        }

        return normalized.substring(index).trim();
    }

    private String extractFirstJsonObject(String value) {
        int start = value.indexOf('{');
        if (start < 0) {
            return value;
        }

        int depth = 0;
        boolean inString = false;
        boolean escape = false;

        for (int i = start; i < value.length(); i++) {
            char c = value.charAt(i);

            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }

            if (c == '"') {
                inString = true;
                continue;
            }

            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return value.substring(start, i + 1);
                }
            }
        }

        int last = value.lastIndexOf('}');
        return last > start ? value.substring(start, last + 1) : value.substring(start);
    }

    private String escapeControlCharsInStrings(String value) {
        StringBuilder out = new StringBuilder(value.length() + 16);

        boolean inString = false;
        boolean escape = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (!inString) {
                out.append(c);
                if (c == '"') {
                    inString = true;
                }
                continue;
            }

            if (escape) {
                out.append(c);
                escape = false;
                continue;
            }

            if (c == '\\') {
                out.append(c);
                escape = true;
                continue;
            }

            if (c == '"') {
                out.append(c);
                inString = false;
                continue;
            }

            if (c < 0x20) {
                appendEscapedControlChar(out, c);
                continue;
            }

            out.append(c);
        }

        return out.toString();
    }

    private void appendEscapedControlChar(StringBuilder out, char c) {
        switch (c) {
            case '\n' -> out.append("\\n");
            case '\r' -> out.append("\\r");
            case '\t' -> out.append("\\t");
            case '\b' -> out.append("\\b");
            case '\f' -> out.append("\\f");
            default -> out.append(String.format("\\u%04x", (int) c));
        }
    }
}
