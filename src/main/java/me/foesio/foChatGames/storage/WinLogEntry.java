package me.foesio.foChatGames.storage;

public record WinLogEntry(String playerName, String game, long timeMs, long createdAt) {
}
