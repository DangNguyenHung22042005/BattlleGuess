package com.battleguess.battleguess.network.response;

import com.battleguess.battleguess.network.Payload;

public class LoginSuccessPayload implements Payload {
    private int playerID;
    private String username;

    public LoginSuccessPayload(int playerID, String username) {
        this.playerID = playerID;
        this.username = username;
    }

    public int getPlayerID() { return playerID; }
    public String getUsername() { return username; }
}
