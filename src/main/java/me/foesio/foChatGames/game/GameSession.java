package me.foesio.foChatGames.game;

public final class GameSession {
    private final GameType type;
    private final String displayName;
    private final String prompt;
    private final String answer;
    private final long startedAtMillis;
    private final boolean custom;

    public GameSession(GameType type, String displayName, String prompt, String answer, boolean custom) {
        this.type = type;
        this.displayName = displayName;
        this.prompt = prompt;
        this.answer = answer;
        this.startedAtMillis = System.currentTimeMillis();
        this.custom = custom;
    }

    public GameType type() {
        return type;
    }

    public String displayName() {
        return displayName;
    }

    public String prompt() {
        return prompt;
    }

    public String answer() {
        return answer;
    }

    public long startedAtMillis() {
        return startedAtMillis;
    }

    public boolean custom() {
        return custom;
    }
}
