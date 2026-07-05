package me.foesio.foChatGames.listener;

import me.foesio.foChatGames.FoChatGames;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {
    private final FoChatGames plugin;

    public PlayerJoinListener(FoChatGames plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.database().ensurePlayer(player.getUniqueId(), player.getName());
    }
}
