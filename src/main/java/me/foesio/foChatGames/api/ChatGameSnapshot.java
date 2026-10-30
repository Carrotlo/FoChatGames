package me.foesio.foChatGames.api;

import java.util.UUID;

/** Immutable public view of a game. The answer is deliberately excluded. */
public record ChatGameSnapshot(UUID id, String gameKey, String displayName, String prompt,
                               long startedAtMillis, boolean custom) {
}
