package me.foesio.foChatGames;

import me.foesio.foChatGames.command.ChatGamesCommand;
import me.foesio.foChatGames.config.ConfigManager;
import me.foesio.foChatGames.editor.EditorService;
import me.foesio.foChatGames.game.GameManager;
import me.foesio.foChatGames.game.WordListManager;
import me.foesio.foChatGames.hook.PlaceholderHook;
import me.foesio.foChatGames.listener.PlayerJoinListener;
import me.foesio.foChatGames.message.LogService;
import me.foesio.foChatGames.message.MessageService;
import me.foesio.foChatGames.storage.Database;
import me.foesio.core.FoCoreContext;
import me.foesio.core.FoPluginCore;
import me.foesio.core.reload.FoReloadRegistry;
import me.foesio.core.reload.FoReloadResult;
import me.foesio.core.update.UpdateNoticeService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class FoChatGames extends JavaPlugin {
    private static final String MODRINTH_PROJECT_ID = "fochatgames";
    private static final int BSTATS_PLUGIN_ID = 32421;

    private FoCoreContext core;
    private ConfigManager configs;
    private MessageService messages;
    private LogService log;
    private Database database;
    private WordListManager wordLists;
    private GameManager games;
    private UpdateNoticeService updates;
    private EditorService editor;
    private FoReloadRegistry reloads;

    @Override
    public void onEnable() {
        core = FoPluginCore.create(this);
        core.metrics(BSTATS_PLUGIN_ID);
        configs = new ConfigManager(this);
        configs.loadAll();
        messages = new MessageService(this);
        log = new LogService(this);
        log.startup(configs.settings().fileLogging);
        database = new Database(this);
        wordLists = new WordListManager(this);
        wordLists.load();
        games = new GameManager(this, wordLists);
        updates = core.createUpdateNotices(messages.updateMessenger(), MODRINTH_PROJECT_ID);
        editor = new EditorService(this);
        reloads = FoReloadRegistry.create()
                .add("config", configs::loadAll)
                .add("file logging", () -> log.reload(configs.settings().fileLogging))
                .add("word lists", wordLists::load)
                .add("games", games::reload)
                .add("editor inputs", editor::refreshInputService);

        try {
            database.open();
        } catch (SQLException exception) {
            log.error("Failed to open userdata.db. Disabling plugin.", exception);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        ChatGamesCommand chatGamesCommand = new ChatGamesCommand(this);
        getCommand("fochatgames").setExecutor(chatGamesCommand);
        getCommand("fochatgames").setTabCompleter(chatGamesCommand);
        getCommand("fochatgamesadmin").setExecutor(chatGamesCommand);
        getCommand("fochatgamesadmin").setTabCompleter(chatGamesCommand);

        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(editor, this);
        registerPlaceholders();

        games.start();
        updates.start();
        log.info("Enabled. Java 21, Spigot/Paper 1.21.x, 26.1.x, and 26.2.x target, SQLite userdata.db.");
    }

    @Override
    public void onDisable() {
        if (games != null) {
            games.stopScheduler();
        }
        if (database != null) {
            database.close();
        }
        if (log != null) {
            log.info("Disabled.");
            log.shutdown();
        }
        if (core != null) {
            core.close();
        }
    }

    public void reloadEverything() {
        log.info("Reload started.");
        FoReloadResult result = reloads.reload();
        if (!result.successful()) {
            log.error("Reload failed at " + result.failedStep() + ": " + result.errorMessage(), result.error());
            throw new IllegalStateException("Reload failed at " + result.failedStep(), result.error());
        }
        log.info("Reload completed.");
    }

    public ConfigManager configs() {
        return configs;
    }

    public MessageService messages() {
        return messages;
    }

    public LogService log() {
        return log;
    }

    public Database database() {
        return database;
    }

    public GameManager games() {
        return games;
    }

    public UpdateNoticeService updates() {
        return updates;
    }

    public EditorService editor() {
        return editor;
    }

    public FoCoreContext core() {
        return core;
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            log.info("PlaceholderAPI not found. Placeholders disabled.");
            return;
        }
        if (new PlaceholderHook(this).register()) {
            log.info("PlaceholderAPI placeholders registered.");
            return;
        }
        log.warning("PlaceholderAPI was found, but placeholder registration failed.");
    }
}
