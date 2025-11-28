package com.battleguess.battleguess.model;

import javafx.beans.property.*;

public class AdminPlayerRow {
    private final IntegerProperty id;
    private final StringProperty username;
    private final IntegerProperty score;
    private final StringProperty status; // "Online" / "Offline"
    private final StringProperty currentRoom; // "Sảnh", "Phòng 10", "-"

    public AdminPlayerRow(int id, String username, int score, String status, String currentRoom) {
        this.id = new SimpleIntegerProperty(id);
        this.username = new SimpleStringProperty(username);
        this.score = new SimpleIntegerProperty(score);
        this.status = new SimpleStringProperty(status);
        this.currentRoom = new SimpleStringProperty(currentRoom);
    }

    // Getters for Property (để TableView dùng)
    public IntegerProperty idProperty() { return id; }
    public StringProperty usernameProperty() { return username; }
    public IntegerProperty scoreProperty() { return score; }
    public StringProperty statusProperty() { return status; }
    public StringProperty currentRoomProperty() { return currentRoom; }

    public int getId() { return id.get(); }
    public String getStatus() { return status.get(); }
}