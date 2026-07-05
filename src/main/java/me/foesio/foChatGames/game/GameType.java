package me.foesio.foChatGames.game;

import java.util.Locale;

public enum GameType {
    COMPLETION("completion", "Completion"),
    GUESS_THE_NUMBER("guess_the_number", "Guess The Number"),
    MATH("math", "Math"),
    TRIVIA("trivia", "Trivia"),
    TYPE("type", "Type"),
    UNREVERSE("unreverse", "Unreverse"),
    UNSCRAMBLE("unscramble", "Unscramble");

    private final String key;
    private final String defaultDisplayName;

    GameType(String key, String defaultDisplayName) {
        this.key = key;
        this.defaultDisplayName = defaultDisplayName;
    }

    public String key() {
        return key;
    }

    public String defaultDisplayName() {
        return defaultDisplayName;
    }

    public static GameType from(String input) {
        if (input == null) {
            return null;
        }

        String normalized = input.toLowerCase(Locale.ROOT).replace('-', '_');
        for (GameType type : values()) {
            if (type.key.equals(normalized) || type.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
