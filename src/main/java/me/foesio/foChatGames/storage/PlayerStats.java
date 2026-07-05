package me.foesio.foChatGames.storage;

import java.util.HashMap;
import java.util.Map;

public final class PlayerStats {
    private long totalWins;
    private long bestRecordMs;
    private final Map<String, Long> winsByGame = new HashMap<>();
    private final Map<String, Long> recordByGame = new HashMap<>();

    public long totalWins() {
        return totalWins;
    }

    public void totalWins(long totalWins) {
        this.totalWins = totalWins;
    }

    public long bestRecordMs() {
        return bestRecordMs;
    }

    public void bestRecordMs(long bestRecordMs) {
        this.bestRecordMs = bestRecordMs;
    }

    public Map<String, Long> winsByGame() {
        return winsByGame;
    }

    public Map<String, Long> recordByGame() {
        return recordByGame;
    }
}
