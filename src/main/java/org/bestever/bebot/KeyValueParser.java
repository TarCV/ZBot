package org.bestever.bebot;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class KeyValueParser {
    private String input;
    private int position = 0;

    private static final Pattern KEY_PATTERN = Pattern.compile("\\S+?(?==)");
    private static final Pattern ASSIGNEMENT_PATTERN = Pattern.compile("\\s*=\\s*");
    private static final Pattern QUOTED_VALUE_PATTERN = Pattern.compile("([\"'])(.*?)\\1");
    private static final Pattern VALUE_PATTERN = Pattern.compile("\\S+");
    private static final Pattern SPACE_PATTERN = Pattern.compile("\\s*");

    private final Matcher matcher;

    public KeyValueParser(String input) {
        this.input = input;
        this.matcher = KEY_PATTERN.matcher(this.input);
    }

    public synchronized Map<String, String> parse() throws InputException {
        if (position != 0) {
            throw new IllegalStateException("This method can only be called once");
        }

        final HashMap<String, String> pairs = new HashMap<>();

        while (position < input.length()) {
            consumeByPattern(SPACE_PATTERN, 0);
            if (position >= input.length()) {
                break;
            }

            final String key = consumeByPattern(KEY_PATTERN, 0).trim();
            if (pairs.containsKey(key)) {
                throw new InputException("Got duplicate key '" + key + "'");
            }

            consumeByPattern(ASSIGNEMENT_PATTERN, 0);

            final String value;
            final char firstValueChar = peekChar();
            if (firstValueChar == '\'' || firstValueChar == '"') {
                value = consumeByPattern(QUOTED_VALUE_PATTERN, 2);
            } else {
                value = consumeByPattern(VALUE_PATTERN, 0);
            }

            pairs.put(key, value);
        }
        return pairs;
    }

    private char peekChar() throws InputException {
        if (position >= input.length()) {
            throw new InputException("Something is wrong with you command - expected more input");
        }
        return input.charAt(position);
    }

    private String consumeByPattern(Pattern pattern, int group) throws InputException {
        matcher
                .usePattern(pattern)
                .region(position, input.length());
        if (!matcher.lookingAt()) {
            throw new InputException(MessageFormat.format("Something is wrong with your command near ''{0}''", describePosition(input, position)));
        }
        position += matcher.group().length(); // can't use end() as it would inlude look-ahead characters
        return matcher.group(group);
    }

    private String describePosition(String message, int position) {
        final int endPosition = Math.min(message.length(), position + 10);
        return message.substring(position, endPosition);
    }
}
