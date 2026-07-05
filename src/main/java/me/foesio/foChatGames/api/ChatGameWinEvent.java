package me.foesio.foChatGames.api;

import me.foesio.foChatGames.game.GameSession;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class ChatGameWinEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final GameSession session;
    private final Player winner;
    private final long timeMs;
    private boolean cancelled;

    public ChatGameWinEvent(GameSession session, Player winner, long timeMs) {
        this.session = session;
        this.winner = winner;
        this.timeMs = timeMs;
    }

    public GameSession session() {
        return session;
    }

    public Player winner() {
        return winner;
    }

    public long timeMs() {
        return timeMs;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
