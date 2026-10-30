package me.foesio.foChatGames.service;

import me.foesio.foChatGames.FoChatGames;
import me.foesio.foChatGames.api.ChatGameControlService;
import me.foesio.foChatGames.api.ChatGameSnapshot;
import me.foesio.foChatGames.api.ChatGameStats;
import me.foesio.foChatGames.api.ChatGamesService;
import me.foesio.foChatGames.game.GameType;
import me.foesio.foChatGames.storage.PlayerStats;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ChatGamesApi implements ChatGamesService, ChatGameControlService {
    private final FoChatGames plugin;

    public ChatGamesApi(FoChatGames plugin) {
        this.plugin = plugin;
    }

    @Override
    public Optional<ChatGameSnapshot> activeGame() {
        requireAvailable();
        return Optional.ofNullable(plugin.games().active()).map(session -> session.snapshot());
    }

    @Override
    public int secondsUntilNext() {
        requireAvailable();
        return plugin.games().secondsUntilNext();
    }

    @Override
    public ChatGameStats stats(UUID playerId) {
        requireAvailable();
        PlayerStats stats = plugin.database().stats(Objects.requireNonNull(playerId, "playerId"));
        return new ChatGameStats(stats.totalWins(), stats.bestRecordMs(),
                stats.winsByGame(), stats.recordByGame());
    }

    @Override
    public StartResult startRandom() {
        requireServerThread();
        return plugin.isEnabled() ? plugin.games().startRandomResult(null, true) : StartResult.UNAVAILABLE;
    }

    @Override
    public StartResult start(String gameKey) {
        requireServerThread();
        if (!plugin.isEnabled()) {
            return StartResult.UNAVAILABLE;
        }
        GameType type = GameType.from(gameKey);
        return type == null ? StartResult.INVALID_GAME : plugin.games().startRandomResult(type, true);
    }

    @Override
    public StartResult startCustom(String gameKey, List<String> arguments) {
        requireServerThread();
        if (!plugin.isEnabled()) {
            return StartResult.UNAVAILABLE;
        }
        GameType type = GameType.from(gameKey);
        return type == null ? StartResult.INVALID_GAME : plugin.games().startCustomResult(type, arguments);
    }

    @Override
    public StopResult stop() {
        requireServerThread();
        if (!plugin.isEnabled()) {
            return StopResult.UNAVAILABLE;
        }
        return plugin.games().stopActive(false) ? StopResult.STOPPED : StopResult.NO_ACTIVE_GAME;
    }

    private void requireAvailable() {
        requireServerThread();
        if (!plugin.isEnabled()) {
            throw new IllegalStateException("FoChatGames is disabled");
        }
    }

    private void requireServerThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("FoChatGames API must be called on the Bukkit server thread");
        }
    }
}
