package me.foesio.foChatGames.api;

import java.util.List;

/** Manual game control. Call on the Bukkit server thread while the provider is enabled. */
public interface ChatGameControlService {
    /** A request result. CANCELLED means a listener cancelled ChatGameStartEvent. */
    enum StartResult {
        STARTED, ALREADY_ACTIVE, INVALID_GAME, INVALID_ARGUMENTS, NO_GAME_AVAILABLE, CANCELLED, UNAVAILABLE
    }

    /** STOPPED means an active game was closed without a winner. */
    enum StopResult {
        STOPPED, NO_ACTIVE_GAME, UNAVAILABLE
    }

    /** Start a weighted random game, like /chatgames start. No game yields NO_GAME_AVAILABLE. */
    StartResult startRandom();

    /** Start a configured game by key, even when it is disabled for automatic selection. */
    StartResult start(String gameKey);

    /** Start a custom game with the same positional arguments as /chatgames custom.
     * Missing or blank arguments yield INVALID_ARGUMENTS. */
    StartResult startCustom(String gameKey, List<String> arguments);

    /** Silently stop the current game, like /chatgames stop. No game yields NO_ACTIVE_GAME. */
    StopResult stop();
}
