package com.rechargeapp.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static Map<String, String> parseObject(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON body is empty.");
        }

        Parser parser = new Parser(json);
        return parser.parseObject();
    }

    public static String quote(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder builder = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (c < 32) {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
            }
        }
        builder.append('"');
        return builder.toString();
    }

    private static final class Parser {
        private final String json;
        private int index;

        private Parser(String json) {
            this.json = json.trim();
        }

        private Map<String, String> parseObject() {
            Map<String, String> values = new LinkedHashMap<>();
            skipWhitespace();
            expect('{');
            skipWhitespace();

            if (peek() == '}') {
                index++;
                return values;
            }

            while (index < json.length()) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                String value = parseValue();
                values.put(key, value);
                skipWhitespace();

                char next = peek();
                if (next == ',') {
                    index++;
                    continue;
                }
                if (next == '}') {
                    index++;
                    break;
                }
                throw new IllegalArgumentException("Invalid JSON object.");
            }

            return values;
        }

        private String parseValue() {
            char c = peek();
            if (c == '"') {
                return parseString();
            }

            int start = index;
            while (index < json.length()) {
                c = json.charAt(index);
                if (c == ',' || c == '}') {
                    break;
                }
                index++;
            }
            String token = json.substring(start, index).trim();
            return "null".equals(token) ? "" : token;
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();

            while (index < json.length()) {
                char c = json.charAt(index++);
                if (c == '"') {
                    return builder.toString();
                }
                if (c == '\\') {
                    if (index >= json.length()) {
                        throw new IllegalArgumentException("Invalid JSON escape.");
                    }
                    char escaped = json.charAt(index++);
                    switch (escaped) {
                        case '"':
                        case '\\':
                        case '/':
                            builder.append(escaped);
                            break;
                        case 'b':
                            builder.append('\b');
                            break;
                        case 'f':
                            builder.append('\f');
                            break;
                        case 'n':
                            builder.append('\n');
                            break;
                        case 'r':
                            builder.append('\r');
                            break;
                        case 't':
                            builder.append('\t');
                            break;
                        case 'u':
                            if (index + 4 > json.length()) {
                                throw new IllegalArgumentException("Invalid JSON unicode escape.");
                            }
                            String hex = json.substring(index, index + 4);
                            builder.append((char) Integer.parseInt(hex, 16));
                            index += 4;
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported JSON escape.");
                    }
                } else {
                    builder.append(c);
                }
            }

            throw new IllegalArgumentException("Unclosed JSON string.");
        }

        private void expect(char expected) {
            if (peek() != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "'.");
            }
            index++;
        }

        private char peek() {
            if (index >= json.length()) {
                return '\0';
            }
            return json.charAt(index);
        }

        private void skipWhitespace() {
            while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
        }
    }
}
