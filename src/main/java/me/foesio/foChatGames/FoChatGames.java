package me.foesio.foChatGames;

import me.foesio.foChatGames.command.ChatGamesCommand;
import me.foesio.foChatGames.api.ChatGameControlService;
import me.foesio.foChatGames.api.ChatGamesService;
import me.foesio.foChatGames.config.ConfigManager;
import me.foesio.foChatGames.editor.EditorService;
import me.foesio.foChatGames.game.GameManager;
import me.foesio.foChatGames.game.WordListManager;
import me.foesio.foChatGames.hook.PlaceholderHook;
import me.foesio.foChatGames.listener.PlayerJoinListener;
import me.foesio.foChatGames.message.LogService;
import me.foesio.foChatGames.message.MessageService;
import me.foesio.foChatGames.service.ChatGamesApi;
import me.foesio.foChatGames.storage.Database;
import me.foesio.core.FoCoreContext;
import me.foesio.core.FoPluginCore;
import me.foesio.core.reload.FoReloadRegistry;
import me.foesio.core.reload.FoReloadResult;
import me.foesio.core.sound.FoAdminSounds;
import me.foesio.core.sound.FoEditorSounds;
import me.foesio.core.sound.FoSoundMigrations;
import me.foesio.core.sound.FoSoundService;
import me.foesio.core.update.UpdateNoticeService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;

import java.sql.SQLException;

public final class FoChatGames extends JavaPlugin {
    private static final String MODRINTH_PROJECT_ID = "fochatgames";
    private static final int BSTATS_PLUGIN_ID = 32421;

    private FoCoreContext core;
    private FoSoundService sounds;
    private FoEditorSounds editorSounds;
    private FoAdminSounds adminSounds;
    private ConfigManager configs;
    private MessageService messages;
    private LogService log;
    private Database database;
    private WordListManager wordLists;
    private GameManager games;
    private UpdateNoticeService updates;
    private EditorService editor;
    private FoReloadRegistry reloads;
    private ChatGamesApi api;

    @Override
    public void onEnable() {
        core = FoPluginCore.create(this);
        sounds = core.createSounds(FoSoundMigrations.create()
                .moveFromConfig("sounds.start", "game.start")
                .moveFromConfig("sounds.win", "game.win")
                .moveFromConfig("sounds.end", "game.end")
                .build());
        editorSounds = FoEditorSounds.create(sounds);
        adminSounds = FoAdminSounds.create(sounds);
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
        updates = core.createUpdateNotices(messages.updateMessenger(), MODRINTH_PROJECT_ID, adminSounds);
        editor = new EditorService(this);
        reloads = FoReloadRegistry.create()
                .add("config", configs::loadAll)
                .add("sounds", sounds::reload)
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
        api = new ChatGamesApi(this);
        Bukkit.getServicesManager().register(ChatGamesService.class, api, this, ServicePriority.Normal);
        Bukkit.getServicesManager().register(ChatGameControlService.class, api, this, ServicePriority.Normal);
        updates.start();
        log.info("Enabled. Java 21, Spigot/Paper 1.21.x, 26.1.x, and 26.2.x target, SQLite userdata.db.");
    }

    @Override
    public void onDisable() {
        if (api != null) {
            Bukkit.getServicesManager().unregister(ChatGamesService.class, api);
            Bukkit.getServicesManager().unregister(ChatGameControlService.class, api);
        }
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

    public FoSoundService sounds() {
        return sounds;
    }

    public FoEditorSounds editorSounds() {
        return editorSounds;
    }

    public FoAdminSounds adminSounds() {
        return adminSounds;
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
