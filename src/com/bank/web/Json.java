package com.bank.web;

import java.util.HashMap;
import java.util.Map;

/** Minimal JSON support (no external library): builds responses, parses flat requests. */
public final class Json {
    private Json() {
    }

    /** Escapes a string for embedding in JSON output. */
    public static String quote(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.append("\"").toString();
    }

    /** Parses a flat JSON object like {"a":"b","n":12} into string values. */
    public static Map<String, String> parseObject(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IllegalArgumentException("Invalid request format.");
        }
        String body = json.substring(1, json.length() - 1).trim();
        int i = 0;
        while (i < body.length()) {
            while (i < body.length()
                    && (Character.isWhitespace(body.charAt(i)) || body.charAt(i) == ',')) {
                i++;
            }
            if (i >= body.length()) {
                break;
            }
            if (body.charAt(i) != '"') {
                throw new IllegalArgumentException("Invalid request format.");
            }
            int[] end = new int[1];
            String key = readString(body, i, end);
            i = end[0];
            while (i < body.length() && Character.isWhitespace(body.charAt(i))) {
                i++;
            }
            if (i >= body.length() || body.charAt(i) != ':') {
                throw new IllegalArgumentException("Invalid request format.");
            }
            i++;
            while (i < body.length() && Character.isWhitespace(body.charAt(i))) {
                i++;
            }
            if (i >= body.length()) {
                throw new IllegalArgumentException("Invalid request format.");
            }
            String value;
            if (body.charAt(i) == '"') {
                value = readString(body, i, end);
                i = end[0];
            } else {
                int start = i;
                while (i < body.length() && body.charAt(i) != ',' && body.charAt(i) != '}') {
                    i++;
                }
                value = body.substring(start, i).trim();
            }
            map.put(key, value);
        }
        return map;
    }

    private static String readString(String s, int start, int[] endOut) {
        StringBuilder out = new StringBuilder();
        int i = start + 1; // skip opening quote
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                switch (n) {
                    case '"': out.append('"'); break;
                    case '\\': out.append('\\'); break;
                    case 'n': out.append('\n'); break;
                    case 'r': out.append('\r'); break;
                    case 't': out.append('\t'); break;
                    default: out.append(n); break;
                }
                i += 2;
            } else if (c == '"') {
                endOut[0] = i + 1;
                return out.toString();
            } else {
                out.append(c);
                i++;
            }
        }
        throw new IllegalArgumentException("Invalid request format.");
    }
}
