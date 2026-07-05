package me.foesio.foChatGames.hook;

import me.foesio.core.placeholder.FoPlaceholders;
import me.foesio.foChatGames.FoChatGames;
import me.foesio.foChatGames.game.GameType;
import me.foesio.foChatGames.storage.PlayerStats;
import me.foesio.foChatGames.util.NumberUtil;
import org.bukkit.OfflinePlayer;

public final class PlaceholderHook {
    private final FoChatGames plugin;

    public PlaceholderHook(FoChatGames plugin) {
        this.plugin = plugin;
    }

    public boolean register() {
        FoPlaceholders placeholders = plugin.core().placeholders("fochatgames")
                .author("Carrotio")
                .logRegistration(false);

        registerGlobal(placeholders);
        registerPlayer(placeholders);
        return placeholders.registerIfAvailable();
    }

    private void registerGlobal(FoPlaceholders placeholders) {
        placeholders
                .global("next", () -> String.valueOf(plugin.games().secondsUntilNext()))
                .global("seconds_until_next", () -> String.valueOf(plugin.games().secondsUntilNext()))
                .global("active", () -> String.valueOf(plugin.games().active() != null))
                .global("active_game", () -> plugin.games().active() == null ? "" : plugin.games().active().type().key());
    }

    private void registerPlayer(FoPlaceholders placeholders) {
        placeholders.offline("enabled", player -> {
            if (player == null) {
                return "";
            }
            String name = player.getName() == null ? "Unknown" : player.getName();
            return String.valueOf(plugin.database().enabled(player.getUniqueId(), name));
        });

        registerTotalStats(placeholders);
        for (GameType type : GameType.values()) {
            registerGameStats(placeholders, type);
        }
    }

    private void registerTotalStats(FoPlaceholders placeholders) {
        for (String key : new String[]{"wins", "won", "chatgames_won", "total_wins", "total_won"}) {
            placeholders.offlineFormattedRaw(
                    key,
                    player -> withStats(player, stats -> NumberUtil.format(stats.totalWins())),
                    player -> withStats(player, stats -> String.valueOf(stats.totalWins()))
            );
        }

        for (String key : new String[]{"record", "best_record", "chatgames_record"}) {
            placeholders.offlineFormattedRaw(
                    key,
                    player -> withStats(player, stats -> NumberUtil.formatSeconds(stats.bestRecordMs() / 1000.0D)),
                    player -> withStats(player, stats -> String.valueOf(stats.bestRecordMs()))
            );
        }
    }

    private void registerGameStats(FoPlaceholders placeholders, GameType type) {
        String game = type.key();
        for (String key : new String[]{"wins_" + game, game + "_wins", "won_" + game, game + "_won", game + "_chatgame_won"}) {
            placeholders.offlineFormattedRaw(
                    key,
                    player -> withStats(player, stats -> NumberUtil.format(stats.winsByGame().getOrDefault(game, 0L))),
                    player -> withStats(player, stats -> String.valueOf(stats.winsByGame().getOrDefault(game, 0L)))
            );
        }

        for (String key : new String[]{"record_" + game, game + "_record", game + "_chatgame_record"}) {
            placeholders.offlineFormattedRaw(
                    key,
                    player -> withStats(player, stats -> NumberUtil.formatSeconds(stats.recordByGame().getOrDefault(game, 0L) / 1000.0D)),
                    player -> withStats(player, stats -> String.valueOf(stats.recordByGame().getOrDefault(game, 0L)))
            );
        }
    }

    private String withStats(OfflinePlayer player, StatValue value) {
        if (player == null) {
            return "";
        }
        return value.resolve(plugin.database().stats(player.getUniqueId()));
    }

    @FunctionalInterface
    private interface StatValue {
        String resolve(PlayerStats stats);
    }
}
