package com.battleguess.battleguess.network.response;

import com.battleguess.battleguess.network.Payload;

public class PlayerMicStatusPayload implements Payload {
    private int playerID;
    private boolean isMicOn;

    public PlayerMicStatusPayload(int playerID, boolean isMicOn) {
        this.playerID = playerID;
        this.isMicOn = isMicOn;
    }

    public int getPlayerID() { return playerID; }
    public boolean isMicOn() { return isMicOn; }
}