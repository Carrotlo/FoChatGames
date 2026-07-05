package me.foesio.foChatGames.api;

import me.foesio.foChatGames.game.GameSession;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class ChatGameEndEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final GameSession session;

    public ChatGameEndEvent(GameSession session) {
        this.session = session;
    }

    public GameSession session() {
        return session;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
