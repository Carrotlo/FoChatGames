package me.foesio.foChatGames.storage;

import me.foesio.foChatGames.FoChatGames;
import me.foesio.foChatGames.game.GameType;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Database {
    private final FoChatGames plugin;
    private Connection connection;

    public Database(FoChatGames plugin) {
        this.plugin = plugin;
    }

    public synchronized void open() throws SQLException {
        File file = new File(plugin.getDataFolder(), "userdata.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("PRAGMA journal_mode=WAL");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS players (" +
                    "uuid TEXT PRIMARY KEY," +
                    "name TEXT NOT NULL," +
                    "enabled INTEGER NOT NULL DEFAULT 1" +
                    ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS stats (" +
                    "uuid TEXT NOT NULL," +
                    "game TEXT NOT NULL," +
                    "wins INTEGER NOT NULL DEFAULT 0," +
                    "best_time_ms INTEGER NOT NULL DEFAULT 0," +
                    "PRIMARY KEY(uuid, game)" +
                    ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS win_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid TEXT NOT NULL," +
                    "name TEXT NOT NULL," +
                    "game TEXT NOT NULL," +
                    "time_ms INTEGER NOT NULL," +
                    "created_at INTEGER NOT NULL" +
                    ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS game_status (" +
                    "game TEXT PRIMARY KEY," +
                    "last_start INTEGER NOT NULL" +
                    ")");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_stats_game_wins ON stats(game, wins)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_stats_game_record ON stats(game, best_time_ms)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_win_logs_uuid_game_id ON win_logs(uuid, game, id)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_win_logs_game_id ON win_logs(game, id)");
        }
    }

    public synchronized void close() {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException exception) {
            plugin.log().error("Failed to close userdata.db", exception);
        }
    }

    public synchronized void ensurePlayer(UUID uuid, String name) {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO players(uuid, name, enabled) VALUES(?, ?, 1) " +
                        "ON CONFLICT(uuid) DO UPDATE SET name=excluded.name")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.log().error("Failed to save player row", exception);
        }
    }

    public synchronized boolean enabled(UUID uuid, String name) {
        ensurePlayer(uuid, name);
        try (PreparedStatement statement = connection.prepareStatement("SELECT enabled FROM players WHERE uuid=?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return !resultSet.next() || resultSet.getInt("enabled") == 1;
            }
        } catch (SQLException exception) {
            plugin.log().error("Failed to read player toggle", exception);
            return true;
        }
    }

    public synchronized boolean toggle(UUID uuid, String name) {
        boolean enabled = !enabled(uuid, name);
        try (PreparedStatement statement = connection.prepareStatement("UPDATE players SET enabled=? WHERE uuid=?")) {
            statement.setInt(1, enabled ? 1 : 0);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.log().error("Failed to save player toggle", exception);
        }
        return enabled;
    }

    public synchronized void recordStart(GameType game) {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO game_status(game, last_start) VALUES(?, ?) " +
                        "ON CONFLICT(game) DO UPDATE SET last_start=excluded.last_start")) {
            statement.setString(1, game.key());
            statement.setLong(2, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.log().error("Failed to save game status", exception);
        }
    }

    public synchronized Map<String, Long> lastStarts() {
        Map<String, Long> starts = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT game, last_start FROM game_status");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                starts.put(resultSet.getString("game"), resultSet.getLong("last_start"));
            }
        } catch (SQLException exception) {
            plugin.log().error("Failed to read game status", exception);
        }
        return starts;
    }

    public synchronized boolean recordWin(UUID uuid, String name, GameType game, long timeMs) {
        ensurePlayer(uuid, name);
        boolean originalAutoCommit;
        try {
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
        } catch (SQLException exception) {
            plugin.log().error("Failed to begin win transaction", exception);
            return false;
        }
        boolean committed = false;
        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO stats(uuid, game, wins, best_time_ms) VALUES(?, ?, 1, ?) " +
                            "ON CONFLICT(uuid, game) DO UPDATE SET " +
                            "wins=wins+1, best_time_ms=CASE WHEN best_time_ms=0 OR excluded.best_time_ms < best_time_ms THEN excluded.best_time_ms ELSE best_time_ms END")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, game.key());
                statement.setLong(3, timeMs);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO win_logs(uuid, name, game, time_ms, created_at) VALUES(?, ?, ?, ?, ?)")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, name);
                statement.setString(3, game.key());
                statement.setLong(4, timeMs);
                statement.setLong(5, System.currentTimeMillis());
                statement.executeUpdate();
            }
            connection.commit();
            committed = true;
        } catch (SQLException exception) {
            plugin.log().error("Failed to save win stats and log", exception);
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                plugin.log().error("Failed to roll back win transaction", rollbackError);
            }
        } finally {
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException exception) {
                plugin.log().error("Failed to restore database auto-commit", exception);
            }
        }
        return committed;
    }

    public synchronized PlayerStats stats(UUID uuid) {
        PlayerStats stats = new PlayerStats();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT game, wins, best_time_ms FROM stats WHERE uuid=?")) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                long totalWins = 0L;
                long best = 0L;
                while (resultSet.next()) {
                    String game = resultSet.getString("game");
                    long wins = resultSet.getLong("wins");
                    long record = resultSet.getLong("best_time_ms");
                    totalWins += wins;
                    if (record > 0L && (best == 0L || record < best)) {
                        best = record;
                    }
                    stats.winsByGame().put(game, wins);
                    stats.recordByGame().put(game, record);
                }
                stats.totalWins(totalWins);
                stats.bestRecordMs(best);
            }
        } catch (SQLException exception) {
            plugin.log().error("Failed to read stats", exception);
        }
        return stats;
    }

    public synchronized List<WinLogEntry> logs(UUID uuid, GameType game, int page, int pageSize) {
        List<WinLogEntry> entries = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT name, game, time_ms, created_at FROM win_logs WHERE 1=1");
        if (uuid != null) {
            sql.append(" AND uuid=?");
        }
        if (game != null) {
            sql.append(" AND game=?");
        }
        sql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            if (uuid != null) {
                statement.setString(index++, uuid.toString());
            }
            if (game != null) {
                statement.setString(index++, game.key());
            }
            statement.setInt(index++, pageSize);
            statement.setInt(index, Math.max(0, page - 1) * pageSize);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(new WinLogEntry(
                            resultSet.getString("name"),
                            resultSet.getString("game"),
                            resultSet.getLong("time_ms"),
                            resultSet.getLong("created_at")
                    ));
                }
            }
        } catch (SQLException exception) {
            plugin.log().error("Failed to read logs", exception);
        }
        return entries;
    }

    public synchronized void reset(UUID uuid, GameType game) {
        String sql = game == null ? "DELETE FROM stats WHERE uuid=?" : "DELETE FROM stats WHERE uuid=? AND game=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            if (game != null) {
                statement.setString(2, game.key());
            }
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.log().error("Failed to reset stats", exception);
        }
    }
}
