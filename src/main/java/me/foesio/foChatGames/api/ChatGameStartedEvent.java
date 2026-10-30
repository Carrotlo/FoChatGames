package me.foesio.foChatGames.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import java.util.Objects;

/** Fired synchronously after a game is active and its timeout has been scheduled. */
public final class ChatGameStartedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ChatGameSnapshot game;

    public ChatGameStartedEvent(ChatGameSnapshot game) {
        this.game = Objects.requireNonNull(game, "game");
    }

    public ChatGameSnapshot game() {
        return game;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
