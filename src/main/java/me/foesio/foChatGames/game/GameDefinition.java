package me.foesio.foChatGames.game;

import java.util.ArrayList;
import java.util.List;

public final class GameDefinition {
    private final GameType type;
    private String displayName;
    private boolean enabled;
    private int chance;
    private int missingPercent;
    private int minimum;
    private int maximum;
    private boolean randomizedText;
    private boolean smartScramble;
    private final List<String> wordLists = new ArrayList<>();
    private final List<String> operators = new ArrayList<>();
    private final List<TriviaQuestion> triviaQuestions = new ArrayList<>();
    private final List<Reward> rewards = new ArrayList<>();

    public GameDefinition(GameType type) {
        this.type = type;
        this.displayName = type.defaultDisplayName();
        this.enabled = true;
        this.chance = 100;
        this.missingPercent = 35;
        this.minimum = 1;
        this.maximum = 100;
        this.smartScramble = true;
    }

    public GameType type() {
        return type;
    }

    public String displayName() {
        return displayName;
    }

    public void displayName(String displayName) {
        this.displayName = displayName == null || displayName.isBlank() ? type.defaultDisplayName() : displayName;
    }

    public boolean enabled() {
        return enabled;
    }

    public void enabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int chance() {
        return chance;
    }

    public void chance(int chance) {
        this.chance = Math.max(0, chance);
    }

    public int missingPercent() {
        return missingPercent;
    }

    public void missingPercent(int missingPercent) {
        this.missingPercent = Math.max(1, Math.min(95, missingPercent));
    }

    public int minimum() {
        return minimum;
    }

    public void minimum(int minimum) {
        this.minimum = minimum;
    }

    public int maximum() {
        return maximum;
    }

    public void maximum(int maximum) {
        this.maximum = maximum;
    }

    public boolean randomizedText() {
        return randomizedText;
    }

    public void randomizedText(boolean randomizedText) {
        this.randomizedText = randomizedText;
    }

    public boolean smartScramble() {
        return smartScramble;
    }

    public void smartScramble(boolean smartScramble) {
        this.smartScramble = smartScramble;
    }

    public List<String> wordLists() {
        return wordLists;
    }

    public List<String> operators() {
        return operators;
    }

    public List<TriviaQuestion> triviaQuestions() {
        return triviaQuestions;
    }

    public List<Reward> rewards() {
        return rewards;
    }
}
