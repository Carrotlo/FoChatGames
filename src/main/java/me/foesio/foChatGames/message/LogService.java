package me.foesio.foChatGames.message;

import me.foesio.core.logging.FoFileLogger;
import me.foesio.foChatGames.FoChatGames;

import java.util.logging.Level;

public final class LogService {
    private final FoChatGames plugin;
    private final FoFileLogger fileLogger;

    public LogService(FoChatGames plugin) {
        this.plugin = plugin;
        this.fileLogger = FoFileLogger.create(plugin);
    }

    public void startup(boolean enabled) {
        fileLogger.configure(enabled, true);
    }

    public void reload(boolean enabled) {
        fileLogger.configure(enabled, false);
    }

    public void shutdown() {
        fileLogger.shutdown();
    }

    public void info(String message) {
        plugin.getLogger().info(prefix(message));
        fileLogger.info(message);
    }

    public void warning(String message) {
        plugin.getLogger().warning(prefix(message));
        fileLogger.warn(message);
    }

    public void error(String message, Throwable throwable) {
        plugin.getLogger().log(Level.SEVERE, prefix(message), throwable);
        fileLogger.error(message, throwable);
    }

    public void debug(String message) {
        if (plugin.configs().settings().debug) {
            info("[debug] " + message);
            return;
        }
        fileLogger.debug(message);
    }

    private String prefix(String message) {
        return "[FoChatGames] " + message;
    }
}
