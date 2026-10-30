package me.foesio.foChatGames.api;

import java.util.Map;

/** Stored win counts and best times in milliseconds, keyed by game key. */
public record ChatGameStats(long totalWins, long bestTimeMillis,
                            Map<String, Long> winsByGame, Map<String, Long> bestTimeMillisByGame) {
    public ChatGameStats {
        winsByGame = Map.copyOf(winsByGame);
        bestTimeMillisByGame = Map.copyOf(bestTimeMillisByGame);
    }
}
