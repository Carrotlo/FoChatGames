package me.foesio.foChatGames.config;

import org.bukkit.event.EventPriority;

import java.util.ArrayList;
import java.util.List;

public final class Settings {
    public boolean enabled = true;
    public boolean autoStart = true;
    public String startTimeSeconds = "180-300";
    public int gameDurationSeconds = 45;
    public int minimumPlayers = 1;
    public boolean caseSensitive = false;
    public boolean notifyWrongCapitalization = true;
    public boolean cancelWinningChat = true;
    public EventPriority chatListenerPriority = EventPriority.NORMAL;
    public boolean debug = false;
    public boolean fileLogging = false;
    public boolean nativeDialogsEnabled = true;
    public boolean nativeDialogsWarnOnFallback = true;
    public String rewardChanceMode = "independent";
    public boolean chatBroadcasts = true;
    public boolean titleBroadcasts = true;
    public boolean subtitleBroadcasts = true;
    public boolean actionbarBroadcasts = true;
    public boolean soundBroadcasts = true;
    public final List<String> worldBlacklist = new ArrayList<>();

    public int nextDelaySeconds() {
        String raw = startTimeSeconds == null ? "180" : startTimeSeconds.trim();
        if (!raw.contains("-")) {
            return positive(parseInt(raw, 180), 180);
        }

        String[] split = raw.split("-", 2);
        int min = positive(parseInt(split[0], 180), 180);
        int max = positive(parseInt(split[1], min), min);
        if (max < min) {
            max = min;
        }
        return min + (int) Math.floor(Math.random() * (max - min + 1));
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int positive(int value, int fallback) {
        return value <= 0 ? fallback : value;
    }
}
