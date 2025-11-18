package com.battleguess.battleguess.network.request;

import com.battleguess.battleguess.network.Payload;

public class PlayerIDPayload implements Payload {
    private int playerID;

    public PlayerIDPayload(int playerID) {
        this.playerID = playerID;
    }

    public int getPlayerID() { return playerID; }
}
