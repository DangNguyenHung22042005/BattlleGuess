package com.battleguess.battleguess.network.response;

import com.battleguess.battleguess.network.Payload;

public class PlayerCameraStatusPayload implements Payload {
    private int playerID;
    private boolean isCameraOn;

    public PlayerCameraStatusPayload(int playerID, boolean isCameraOn) {
        this.playerID = playerID; this.isCameraOn = isCameraOn;
    }

    public int getPlayerID() { return playerID; }
    public boolean isCameraOn() { return isCameraOn; }
}