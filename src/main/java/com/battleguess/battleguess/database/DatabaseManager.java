package com.battleguess.battleguess.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static final String URL = "jdbc:sqlserver://HUNG\\SQLEXPRESS:1433;databaseName=BATTLEGUESS;encrypt=true;trustServerCertificate=true;";
    private static final String USER = "sa";
    private static final String PASSWORD = "123456789";

    private Connection connection;

    public DatabaseManager() throws SQLException {
        connect();
    }

    private void connect() throws SQLException {
        connection = DriverManager.getConnection(URL, USER, PASSWORD);
        System.out.println("✅ Connected to SQL Server successfully!");
    }

    public Connection getConnection() {
        return connection;
    }

    public void addPlayer(String name, String password) throws SQLException {
        String sql = "INSERT INTO Player (name, password) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, password);
            ps.executeUpdate();
        }
    }

    public boolean playerExists(String name) throws SQLException {
        String sql = "SELECT 1 FROM Player WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int getPlayerId(String name) throws SQLException {
        String sql = "SELECT id FROM Player WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        }
        return -1;
    }

    public void updatePlayerScore(int playerId, int newScore) throws SQLException {
        String sql = "UPDATE Player SET score = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, newScore);
            ps.setInt(2, playerId);
            ps.executeUpdate();
        }
    }

    // ---- ROOM CRUD ----
    public int addRoom(String name) throws SQLException {
        String sql = "INSERT INTO Room (name) OUTPUT INSERTED.id VALUES (?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    public boolean roomExists(String name) throws SQLException {
        String sql = "SELECT 1 FROM Room WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ---- PLAYER-ROOM CRUD ----
    public void addPlayerToRoom(int playerId, int roomId, boolean isKeyHolder) throws SQLException {
        String sql = "INSERT INTO PlayerRoom (player_id, room_id, is_key_holder) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            ps.setInt(2, roomId);
            ps.setBoolean(3, isKeyHolder);
            ps.executeUpdate();
        }
    }

    public List<Integer> getPlayersInRoom(int roomId) throws SQLException {
        List<Integer> playerIds = new ArrayList<>();
        String sql = "SELECT player_id FROM PlayerRoom WHERE room_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    playerIds.add(rs.getInt("player_id"));
                }
            }
        }
        return playerIds;
    }

    public void removePlayerFromRoom(int playerId, int roomId) throws SQLException {
        String sql = "DELETE FROM PlayerRoom WHERE player_id = ? AND room_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            ps.setInt(2, roomId);
            ps.executeUpdate();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
