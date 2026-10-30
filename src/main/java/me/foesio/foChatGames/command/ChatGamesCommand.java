package me.foesio.foChatGames.command;

import me.foesio.foChatGames.FoChatGames;
import me.foesio.foChatGames.game.GameType;
import me.foesio.foChatGames.game.GameManager;
import me.foesio.foChatGames.storage.PlayerStats;
import me.foesio.foChatGames.storage.WinLogEntry;
import me.foesio.foChatGames.util.CommandTokenizer;
import me.foesio.foChatGames.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ChatGamesCommand implements TabExecutor {
    private final FoChatGames plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public ChatGamesCommand(FoChatGames plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            help(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!List.of("help", "statistics", "stats").contains(sub)) {
            plugin.log().info("Command used by " + sender.getName() + ": /" + label + " " + String.join(" ", args));
        }
        switch (sub) {
            case "start" -> start(sender, args);
            case "custom" -> custom(sender, args);
            case "stop" -> stop(sender, args);
            case "stopall" -> stopAll(sender);
            case "statistics", "stats" -> statistics(sender, args);
            case "toggle" -> toggle(sender);
            case "status" -> status(sender);
            case "logs" -> logs(sender, args);
            case "reset" -> reset(sender, args);
            case "version" -> version(sender);
            case "reload" -> reload(sender);
            case "editor" -> editor(sender);
            default -> {
                plugin.messages().send(sender, "general.bad-syntax", Map.of("usage", "/" + label + " help"));
                playCommandError(sender);
            }
        }
        return true;
    }

    private void help(CommandSender sender) {
        if (!sender.hasPermission("fochatgames.help")) {
            plugin.messages().send(sender, "general.no-permission");
            playCommandError(sender);
            return;
        }
        plugin.messages().send(sender, "help.header");
        plugin.messages().sendList(sender, "help.player", Map.of());
        if (sender.hasPermission("fochatgames.admin") || sender.hasPermission("fochatgames.start")) {
            plugin.messages().sendList(sender, "help.admin", Map.of());
        }
    }

    private void start(CommandSender sender, String[] args) {
        if (!require(sender, "fochatgames.start")) {
            return;
        }
        GameType type = args.length >= 2 ? GameType.from(args[1]) : null;
        if (args.length >= 2 && type == null) {
            plugin.log().warning("Manual start failed for " + sender.getName() + ": invalid game " + args[1]);
            plugin.messages().send(sender, "general.invalid-game", Map.of("game", args[1]));
            playCommandError(sender);
            return;
        }
        if (!plugin.games().startRandom(type, true)) {
            plugin.log().warning("Manual start failed for " + sender.getName() + ": active game " + activeName());
            plugin.messages().send(sender, "game.already-active", Map.of("game", activeName()));
            playCommandError(sender);
            return;
        }
        plugin.messages().send(sender, "game.started-manual", Map.of("game", type == null ? "random" : type.key()));
    }

    private void custom(CommandSender sender, String[] args) {
        if (!require(sender, "fochatgames.custom")) {
            return;
        }
        if (args.length < 3) {
            plugin.log().warning("Custom game failed for " + sender.getName() + ": missing arguments.");
            plugin.messages().send(sender, "general.bad-syntax", Map.of("usage", "/chatgames custom <game> <settings...>"));
            playCommandError(sender);
            return;
        }
        GameType type = GameType.from(args[1]);
        if (type == null) {
            plugin.log().warning("Custom game failed for " + sender.getName() + ": invalid game " + args[1]);
            plugin.messages().send(sender, "general.invalid-game", Map.of("game", args[1]));
            playCommandError(sender);
            return;
        }
        String joined = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        List<String> tokens = CommandTokenizer.tokenize(joined);
        String usage = customUsage(type);
        if (!GameManager.validCustomArguments(type, tokens)) {
            plugin.log().warning("Custom game failed for " + sender.getName() + ": bad syntax for " + type.key());
            plugin.messages().send(sender, "general.bad-syntax", Map.of("usage", usage));
            playCommandError(sender);
            return;
        }
        if (!plugin.games().startCustom(type, tokens)) {
            plugin.log().warning("Custom game failed for " + sender.getName() + ": active game " + activeName());
            plugin.messages().send(sender, "game.already-active", Map.of("game", activeName()));
            playCommandError(sender);
            return;
        }
        plugin.messages().send(sender, "game.custom-started", Map.of("game", type.key()));
    }

    private void stop(CommandSender sender, String[] args) {
        if (!require(sender, "fochatgames.stop")) {
            return;
        }
        if (plugin.games().active() == null) {
            plugin.messages().send(sender, "general.no-active-game");
            playCommandError(sender);
            return;
        }
        if (args.length >= 2) {
            GameType type = GameType.from(args[1]);
            if (type == null || plugin.games().active().type() != type) {
                plugin.messages().send(sender, "general.no-active-game");
                playCommandError(sender);
                return;
            }
        }
        String game = plugin.games().active().displayName();
        plugin.games().stopActive(false);
        plugin.log().info("Game stopped by " + sender.getName() + ": " + game);
        plugin.messages().send(sender, "general.stopped", Map.of("game", game));
    }

    private void stopAll(CommandSender sender) {
        if (!require(sender, "fochatgames.stopall")) {
            return;
        }
        plugin.games().stopActive(false);
        plugin.log().info("All games stopped by " + sender.getName() + ".");
        plugin.messages().send(sender, "general.stopped-all");
    }

    private void statistics(CommandSender sender, String[] args) {
        if (!require(sender, "fochatgames.statistics")) {
            return;
        }
        OfflinePlayer target;
        if (args.length >= 2) {
            if (!sender.hasPermission("fochatgames.statistics.others")) {
                plugin.messages().send(sender, "general.no-permission");
                playCommandError(sender);
                return;
            }
            target = Bukkit.getOfflinePlayer(args[1]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            plugin.messages().send(sender, "general.bad-syntax", Map.of("usage", "/chatgames statistics <player>"));
            playCommandError(sender);
            return;
        }

        UUID uuid = target.getUniqueId();
        String name = target.getName() == null ? "Unknown" : target.getName();
        PlayerStats stats = plugin.database().stats(uuid);
        plugin.messages().send(sender, "stats.header", Map.of("player", name));
        plugin.messages().send(sender, "stats.line-total", Map.of(
                "wins", NumberUtil.format(stats.totalWins()),
                "record", NumberUtil.formatSeconds(stats.bestRecordMs() / 1000.0D)
        ));
        for (GameType type : GameType.values()) {
            plugin.messages().send(sender, "stats.line-game", Map.of(
                    "game", type.defaultDisplayName(),
                    "wins", NumberUtil.format(stats.winsByGame().getOrDefault(type.key(), 0L)),
                    "record", NumberUtil.formatSeconds(stats.recordByGame().getOrDefault(type.key(), 0L) / 1000.0D)
            ));
        }
    }

    private void toggle(CommandSender sender) {
        if (!require(sender, "fochatgames.toggle")) {
            return;
        }
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "general.player-only");
            return;
        }
        boolean enabled = plugin.database().toggle(player.getUniqueId(), player.getName());
        plugin.log().info("Player toggled chat games: " + player.getName() + " -> " + enabled);
        plugin.messages().send(player, enabled ? "game.toggle-on" : "game.toggle-off");
    }

    private void status(CommandSender sender) {
        if (!require(sender, "fochatgames.status")) {
            return;
        }
        Map<String, Long> starts = plugin.database().lastStarts();
        plugin.messages().send(sender, "status.header");
        for (GameType type : GameType.values()) {
            plugin.messages().send(sender, "status.line", Map.of(
                    "game", type.defaultDisplayName(),
                    "last", plugin.games().formatLast(starts.getOrDefault(type.key(), 0L))
            ));
        }
    }

    private void logs(CommandSender sender, String[] args) {
        if (!require(sender, "fochatgames.logs")) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "general.bad-syntax", Map.of("usage", "/chatgames logs <global/player> ..."));
            playCommandError(sender);
            return;
        }
        UUID uuid = null;
        int page = 1;
        GameType game = null;
        if (args[1].equalsIgnoreCase("player")) {
            if (args.length < 3) {
                plugin.messages().send(sender, "general.bad-syntax", Map.of("usage", "/chatgames logs player <player> [page] [game]"));
                playCommandError(sender);
                return;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
            uuid = target.getUniqueId();
            if (args.length >= 4) {
                page = parsePage(args[3]);
            }
            if (args.length >= 5) {
                game = GameType.from(args[4]);
            }
        } else {
            if (args.length >= 3) {
                page = parsePage(args[2]);
            }
            if (args.length >= 4) {
                game = GameType.from(args[3]);
            }
        }
        List<WinLogEntry> logs = plugin.database().logs(uuid, game, page, 10);
        plugin.messages().send(sender, "logs.header", Map.of("page", String.valueOf(page)));
        if (logs.isEmpty()) {
            plugin.messages().send(sender, "logs.empty");
            return;
        }
        for (WinLogEntry entry : logs) {
            plugin.messages().send(sender, "logs.line", Map.of(
                    "date", dateFormat.format(new Date(entry.createdAt())),
                    "player", entry.playerName(),
                    "game", entry.game(),
                    "time", NumberUtil.formatSeconds(entry.timeMs() / 1000.0D)
            ));
        }
    }

    private void reset(CommandSender sender, String[] args) {
        if (!require(sender, "fochatgames.reset")) {
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(sender, "general.bad-syntax", Map.of("usage", "/chatgames reset <player> [game]"));
            playCommandError(sender);
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        GameType game = args.length >= 3 ? GameType.from(args[2]) : null;
        plugin.database().reset(target.getUniqueId(), game);
        plugin.log().warning("Stats reset by " + sender.getName() + ": " + (target.getName() == null ? args[1] : target.getName()) + (game == null ? " all games" : " " + game.key()));
        plugin.messages().send(sender, "stats.reset", Map.of("player", target.getName() == null ? args[1] : target.getName()));
    }

    private void version(CommandSender sender) {
        if (!require(sender, "fochatgames.admin")) {
            return;
        }
        plugin.log().info("Version check requested by " + sender.getName() + ".");
        plugin.updates().checkAndSendVersion(sender);
    }

    private void reload(CommandSender sender) {
        if (!require(sender, "fochatgames.admin")) {
            return;
        }
        plugin.reloadEverything();
        if (sender instanceof Player player) {
            plugin.adminSounds().reload(player);
        }
        plugin.messages().send(sender, "general.reloaded");
    }

    private void editor(CommandSender sender) {
        if (!require(sender, "fochatgames.admin")) {
            return;
        }
        if (!(sender instanceof Player player)) {
            plugin.log().warning("Editor open failed for " + sender.getName() + ": sender is not a player.");
            plugin.messages().send(sender, "general.player-only");
            return;
        }
        plugin.log().info("Editor opened by " + player.getName() + ".");
        plugin.editor().openMain(player);
        plugin.messages().send(player, "editor.opened");
    }

    private boolean require(CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("fochatgames.admin")) {
            return true;
        }
        plugin.log().warning("Command denied for " + sender.getName() + ": missing " + permission);
        plugin.messages().send(sender, "general.no-permission");
        playCommandError(sender);
        return false;
    }

    private void playCommandError(CommandSender sender) {
        plugin.adminSounds().updateError(sender);
    }

    private int parsePage(String raw) {
        Long value = NumberUtil.parseLarge(raw);
        if (value == null || value <= 0L) {
            return 1;
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : value.intValue();
    }

    private String activeName() {
        return plugin.games().active() == null ? "none" : plugin.games().active().displayName();
    }

    private String customUsage(GameType type) {
        return switch (type) {
            case COMPLETION -> "/chatgames custom completion <word> [missing percent]";
            case GUESS_THE_NUMBER -> "/chatgames custom guess_the_number <minimum> <maximum> [answer]";
            case MATH -> "/chatgames custom math <math hint> <answer>";
            case TRIVIA -> "/chatgames custom trivia <question> <answer>";
            case TYPE -> "/chatgames custom type <word>";
            case UNREVERSE -> "/chatgames custom unreverse <word>";
            case UNSCRAMBLE -> "/chatgames custom unscramble <word>";
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(visibleSubcommands(sender), args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && List.of("start", "custom", "stop").contains(sub)) {
            return filter(gameKeys(), args[1]);
        }
        if (args.length == 2 && sub.equals("logs")) {
            return filter(List.of("global", "player"), args[1]);
        }
        if (sub.equals("logs")) {
            return logsCompletions(args);
        }
        if (sub.equals("statistics") || sub.equals("stats")) {
            return args.length == 2 ? filter(onlinePlayerNames(), args[1]) : List.of();
        }
        if (sub.equals("reset")) {
            if (args.length == 2) {
                return filter(onlinePlayerNames(), args[1]);
            }
            if (args.length == 3) {
                return filter(gameKeys(), args[2]);
            }
        }
        return List.of();
    }

    private List<String> visibleSubcommands(CommandSender sender) {
        LinkedHashSet<String> options = new LinkedHashSet<>();
        addIf(sender, options, "help", "fochatgames.help");
        addIf(sender, options, "start", "fochatgames.start");
        addIf(sender, options, "custom", "fochatgames.custom");
        addIf(sender, options, "stop", "fochatgames.stop");
        addIf(sender, options, "stopall", "fochatgames.stopall");
        addIf(sender, options, "statistics", "fochatgames.statistics");
        addIf(sender, options, "stats", "fochatgames.statistics");
        addIf(sender, options, "toggle", "fochatgames.toggle");
        addIf(sender, options, "status", "fochatgames.status");
        addIf(sender, options, "logs", "fochatgames.logs");
        addIf(sender, options, "reset", "fochatgames.reset");
        addIf(sender, options, "version", "fochatgames.admin");
        addIf(sender, options, "reload", "fochatgames.admin");
        addIf(sender, options, "editor", "fochatgames.admin");
        return new ArrayList<>(options);
    }

    private void addIf(CommandSender sender, LinkedHashSet<String> options, String option, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("fochatgames.admin")) {
            options.add(option);
        }
    }

    private List<String> logsCompletions(String[] args) {
        if (args[1].equalsIgnoreCase("player")) {
            if (args.length == 3) {
                return filter(onlinePlayerNames(), args[2]);
            }
            if (args.length == 5) {
                return filter(gameKeys(), args[4]);
            }
            return List.of();
        }
        if (args[1].equalsIgnoreCase("global") && args.length == 4) {
            return filter(gameKeys(), args[3]);
        }
        return List.of();
    }

    private List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private List<String> gameKeys() {
        List<String> keys = new ArrayList<>();
        for (GameType type : GameType.values()) {
            keys.add(type.key());
        }
        return keys;
    }

    private List<String> filter(List<String> options, String input) {
        String lower = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
