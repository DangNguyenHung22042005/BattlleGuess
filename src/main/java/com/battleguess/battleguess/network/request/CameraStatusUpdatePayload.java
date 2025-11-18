package com.battleguess.battleguess.network.request;

import com.battleguess.battleguess.network.Payload;

public class CameraStatusUpdatePayload implements Payload {
    private int playerID;
    private int roomID;
    private boolean isCameraOn;

    public CameraStatusUpdatePayload(int playerID, int roomID, boolean isCameraOn) {
        this.playerID = playerID; this.roomID = roomID; this.isCameraOn = isCameraOn;
    }

    public int getPlayerID() { return playerID; }
    public int getRoomID() { return roomID; }
    public boolean isCameraOn() { return isCameraOn; }
}