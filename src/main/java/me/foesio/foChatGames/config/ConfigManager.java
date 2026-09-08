package me.foesio.foChatGames.config;

import me.foesio.foChatGames.FoChatGames;
import me.foesio.foChatGames.game.GameDefinition;
import me.foesio.foChatGames.game.GameType;
import me.foesio.foChatGames.game.Reward;
import me.foesio.foChatGames.game.TriviaQuestion;
import me.foesio.core.config.ResourceFiles;
import me.foesio.core.message.FoMessageService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventPriority;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConfigManager {
    private final FoChatGames plugin;
    private final Map<GameType, GameDefinition> games = new EnumMap<>(GameType.class);
    private final Map<GameType, File> gameFiles = new EnumMap<>(GameType.class);
    private final Map<GameType, YamlConfiguration> gameConfigs = new EnumMap<>(GameType.class);
    private Settings settings = new Settings();
    private FileConfiguration messages;

    public ConfigManager(FoChatGames plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        ensureDefaults();
        plugin.reloadConfig();
        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
        migrateSprites();
        migrateTitlePrefixes();
        loadSettings();
        loadGames();
    }

    public void saveConfigValue(String path, Object value) throws IOException {
        plugin.getConfig().set(path, value);
        plugin.getConfig().save(new File(plugin.getDataFolder(), "config.yml"));
        loadSettings();
    }

    public void saveGameValue(GameType type, String path, Object value) throws IOException {
        YamlConfiguration config = gameConfigs.get(type);
        File file = gameFiles.get(type);
        if (config == null || file == null) {
            throw new IOException("Missing game config for " + type.key());
        }
        config.set(path, value);
        config.save(file);
        loadGames();
    }

    public Settings settings() {
        return settings;
    }

    public FileConfiguration messages() {
        return messages;
    }

    public Map<GameType, GameDefinition> games() {
        return games;
    }

    public GameDefinition game(GameType type) {
        return games.get(type);
    }

    private void ensureDefaults() {
        plugin.saveDefaultConfig();
        saveIfMissing("messages.yml");
        saveIfMissing("word-lists/example.txt");
        for (GameType type : GameType.values()) {
            saveIfMissing("games/" + type.key() + ".yml");
        }
    }

    private void saveIfMissing(String path) {
        ResourceFiles.saveDefault(plugin, path);
    }

    private void migrateSprites() {
        plugin.core().migrations().runToVersion(1, () -> {
            boolean changed = false;
            changed |= FoMessageService.addMissingToken(messages, "tokens.prefix", ":paper:", null);
            changed |= FoMessageService.addMissingToken(messages, "general.reloaded", ":emerald:");
            changed |= FoMessageService.addMissingToken(messages, "general.saved", ":emerald:");
            changed |= FoMessageService.addMissingToken(messages, "general.save-failed", ":redstone:");
            changed |= FoMessageService.addMissingToken(messages, "game.started-manual", ":clock:");
            changed |= FoMessageService.addMissingToken(messages, "game.custom-started", ":clock:");
            changed |= FoMessageService.addMissingToken(messages, "game.win.chat", ":emerald:");
            changed |= FoMessageService.addMissingToken(messages, "editor.opened", ":book:");
            changed |= FoMessageService.addMissingToken(messages, "editor.reward-added", ":emerald:");
            changed |= FoMessageService.addMissingToken(messages, "editor.reward-deleted", ":lava_bucket:");
            if (!changed) {
                return true;
            }
            try {
                messages.save(new File(plugin.getDataFolder(), "messages.yml"));
                return true;
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not save sprite migration: " + exception.getMessage());
                return false;
            }
        });
    }

    private void migrateTitlePrefixes() {
        plugin.core().migrations().runToVersion(2, () -> {
            boolean changed = false;
            for (String basePath : List.of("game.start", "game.win", "game.end")) {
                for (String channel : List.of("title", "subtitle")) {
                    String path = basePath + "." + channel;
                    String value = messages.getString(path, null);
                    if (value == null || !value.contains("{prefix}")) {
                        continue;
                    }
                    String updated = value.replace("{prefix}", "").replaceFirst("^[ \\t]+", "");
                    if (!updated.equals(value)) {
                        messages.set(path, updated);
                        changed = true;
                    }
                }
            }
            if (!changed) {
                return true;
            }
            try {
                messages.save(new File(plugin.getDataFolder(), "messages.yml"));
                return true;
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not save title-prefix migration: " + exception.getMessage());
                return false;
            }
        });
    }

    private void loadSettings() {
        Settings loaded = new Settings();
        FileConfiguration config = plugin.getConfig();
        loaded.enabled = config.getBoolean("general.enabled", true);
        loaded.autoStart = config.getBoolean("general.auto-start", true);
        loaded.startTimeSeconds = config.getString("general.start-time-seconds", "180-300");
        loaded.gameDurationSeconds = Math.max(5, config.getInt("general.game-duration-seconds", 45));
        loaded.minimumPlayers = Math.max(1, config.getInt("general.minimum-players", 1));
        loaded.caseSensitive = config.getBoolean("general.case-sensitive", false);
        loaded.notifyWrongCapitalization = config.getBoolean("general.notify-wrong-capitalization", true);
        loaded.cancelWinningChat = config.getBoolean("general.cancel-winning-chat", true);
        loaded.chatListenerPriority = parsePriority(config.getString("general.chat-listener-priority", "NORMAL"));
        loaded.debug = config.getBoolean("general.debug", false);
        loaded.fileLogging = config.getBoolean("general.file-logging", false);
        loaded.nativeDialogsEnabled = config.getBoolean("native-dialogs.enabled", true);
        loaded.nativeDialogsWarnOnFallback = config.getBoolean("native-dialogs.warn-on-fallback", true);
        loaded.rewardChanceMode = config.getString("rewards.chance-mode", "independent").toLowerCase(Locale.ROOT);
        loaded.chatBroadcasts = config.getBoolean("broadcasts.chat", true);
        loaded.titleBroadcasts = config.getBoolean("broadcasts.titles", true);
        loaded.subtitleBroadcasts = config.getBoolean("broadcasts.subtitles", true);
        loaded.actionbarBroadcasts = config.getBoolean("broadcasts.actionbar", true);
        loaded.soundBroadcasts = config.getBoolean("broadcasts.sounds", true);
        loaded.worldBlacklist.addAll(config.getStringList("world-blacklist").stream()
                .map(world -> world.toLowerCase(Locale.ROOT))
                .toList());
        settings = loaded;
    }

    private EventPriority parsePriority(String raw) {
        if (raw == null || raw.isBlank()) {
            plugin.getLogger().warning("Invalid chat listener priority: " + raw + ". Using NORMAL.");
            return EventPriority.NORMAL;
        }
        try {
            return EventPriority.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid chat listener priority: " + raw + ". Using NORMAL.");
            return EventPriority.NORMAL;
        }
    }

    private void loadGames() {
        games.clear();
        gameFiles.clear();
        gameConfigs.clear();

        for (GameType type : GameType.values()) {
            File file = new File(plugin.getDataFolder(), "games/" + type.key() + ".yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            GameDefinition definition = new GameDefinition(type);
            definition.enabled(config.getBoolean("enabled", true));
            definition.chance(config.getInt("chance", 100));
            definition.displayName(config.getString("display-name", type.defaultDisplayName()));
            definition.missingPercent(config.getInt("missing-percent", 35));
            definition.minimum(config.getInt("minimum", 1));
            definition.maximum(config.getInt("maximum", 100));
            definition.randomizedText(config.getBoolean("randomized-text", false));
            definition.smartScramble(config.getBoolean("smart-scramble", true));
            definition.wordLists().addAll(config.getStringList("word-lists"));
            definition.operators().addAll(config.getStringList("operators"));
            loadTrivia(config, definition);
            loadRewards(config, definition);
            games.put(type, definition);
            gameFiles.put(type, file);
            gameConfigs.put(type, config);
        }
    }

    private void loadTrivia(YamlConfiguration config, GameDefinition definition) {
        List<Map<?, ?>> rawQuestions = config.getMapList("questions");
        for (Map<?, ?> raw : rawQuestions) {
            Object question = raw.get("question");
            Object answer = raw.get("answer");
            if (question != null && answer != null) {
                definition.triviaQuestions().add(new TriviaQuestion(String.valueOf(question), String.valueOf(answer)));
            }
        }
    }

    private void loadRewards(YamlConfiguration config, GameDefinition definition) {
        List<Map<?, ?>> rawRewards = config.getMapList("rewards");
        for (int index = 0; index < rawRewards.size(); index++) {
            Map<?, ?> raw = rawRewards.get(index);
            YamlConfiguration temp = new YamlConfiguration();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                temp.set(String.valueOf(entry.getKey()), entry.getValue());
            }
            String context = definition.type().key() + ".rewards[" + index + "]";
            definition.rewards().add(Reward.from(temp, plugin.getLogger()::warning, context));
        }
    }

    public List<Map<String, Object>> serializableRewards(GameType type) {
        YamlConfiguration config = gameConfigs.get(type);
        if (config == null) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> rewards = new ArrayList<>();
        for (Map<?, ?> reward : config.getMapList("rewards")) {
            Map<String, Object> copy = new java.util.LinkedHashMap<>();
            reward.forEach((key, value) -> copy.put(String.valueOf(key), value));
            rewards.add(copy);
        }
        return rewards;
    }
}
