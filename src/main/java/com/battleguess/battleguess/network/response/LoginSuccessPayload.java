package com.battleguess.battleguess.network.response;

import com.battleguess.battleguess.network.Payload;

public class LoginSuccessPayload implements Payload {
    private int playerID;
    private String username;
    private int score;

    public LoginSuccessPayload(int playerID, String username, int score) {
        this.playerID = playerID;
        this.username = username;
        this.score = score;
    }

    public int getPlayerID() { return playerID; }
    public String getUsername() { return username; }
    public int getScore() { return score; }
}
