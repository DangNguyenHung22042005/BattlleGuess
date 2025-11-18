package com.battleguess.battleguess.model;

import java.io.Serializable;

public class PlayerState implements Serializable {
    private int playerID;
    private String username;
    private int score;
    private boolean isOwner;
    private boolean isOnline;

    public PlayerState(int playerID, String username, int score, boolean isOwner, boolean isOnline) {
        this.playerID = playerID;
        this.username = username;
        this.score = score;
        this.isOwner = isOwner;
        this.isOnline = isOnline;
    }

    public int getPlayerID() { return playerID; }
    public String getUsername() { return username; }
    public int getScore() { return score; }
    public boolean isOwner() { return isOwner; }
    public boolean isOnline() { return isOnline; }

    @Override
    public String toString() {
        return username + " (Score: " + score + ") - " + (isOnline ? "Online" : "Offline");
    }
}