package me.foesio.foChatGames.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;
import java.util.Objects;

/** Fired synchronously after a game has closed, for wins, timeouts, and manual stops. */
public final class ChatGameFinishedEvent extends Event {
    public enum Outcome { WIN, TIMEOUT, STOPPED }

    private static final HandlerList HANDLERS = new HandlerList();
    private final ChatGameSnapshot game;
    private final Outcome outcome;
    private final UUID winnerId;
    private final String winnerName;
    private final long elapsedMillis;
    private final boolean statsPersisted;

    public ChatGameFinishedEvent(ChatGameSnapshot game, Outcome outcome, UUID winnerId,
                                 String winnerName, long elapsedMillis, boolean statsPersisted) {
        this.game = Objects.requireNonNull(game, "game");
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        if ((outcome == Outcome.WIN && (winnerId == null || winnerName == null))
                || (outcome != Outcome.WIN && (winnerId != null || winnerName != null))) {
            throw new IllegalArgumentException("Winner ID and name are required exactly for WIN");
        }
        if (elapsedMillis < 0L || (outcome != Outcome.WIN && statsPersisted)) {
            throw new IllegalArgumentException("Invalid finished-game result");
        }
        this.winnerId = winnerId;
        this.winnerName = winnerName;
        this.elapsedMillis = elapsedMillis;
        this.statsPersisted = statsPersisted;
    }

    public ChatGameSnapshot game() { return game; }
    public Outcome outcome() { return outcome; }
    /** Null unless the outcome is WIN. */
    public UUID winnerId() { return winnerId; }
    /** Null unless the outcome is WIN. */
    public String winnerName() { return winnerName; }
    public long elapsedMillis() { return elapsedMillis; }
    /** True only when a win's statistics and log were committed to storage. */
    public boolean statsPersisted() { return statsPersisted; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
