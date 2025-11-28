package com.battleguess.battleguess.model;

import javafx.beans.property.*;

public class AdminRoomMemberRow {
    private final IntegerProperty playerID;
    private final StringProperty username;
    private final IntegerProperty score;
    private final BooleanProperty isOwner;
    private final StringProperty status; // "Online" / "Offline"

    public AdminRoomMemberRow(int playerID, String username, int score, boolean isOwner, boolean isOnline) {
        this.playerID = new SimpleIntegerProperty(playerID);
        this.username = new SimpleStringProperty(username);
        this.score = new SimpleIntegerProperty(score);
        this.isOwner = new SimpleBooleanProperty(isOwner);
        this.status = new SimpleStringProperty(isOnline ? "Online" : "Offline");
    }

    public IntegerProperty playerIDProperty() { return playerID; }
    public StringProperty usernameProperty() { return username; }
    public IntegerProperty scoreProperty() { return score; }
    public BooleanProperty isOwnerProperty() { return isOwner; }
    public StringProperty statusProperty() { return status; }

    public int getPlayerID() { return playerID.get(); }
    public String getUsername() { return username.get(); }
    public boolean isOwner() { return isOwner.get(); }
}