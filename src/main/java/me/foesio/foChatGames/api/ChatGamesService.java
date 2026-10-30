package me.foesio.foChatGames.api;

import java.util.Optional;
import java.util.UUID;

/** Read-only FoChatGames service. Call on the Bukkit server thread while the provider is enabled. */
public interface ChatGamesService {
    /** Current game, or empty when none is active. */
    Optional<ChatGameSnapshot> activeGame();

    /** Seconds until the next automatic attempt; zero when automatic scheduling is disabled. */
    int secondsUntilNext();

    /** Stored statistics for a player, including an offline player. Missing rows yield zero values. */
    ChatGameStats stats(UUID playerId);
}
