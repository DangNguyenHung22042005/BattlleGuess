package com.battleguess.battleguess.network.request;

import com.battleguess.battleguess.network.Payload;

public class MicStatusUpdatePayload implements Payload {
    private int playerID;
    private int roomID;
    private boolean isMicOn;

    public MicStatusUpdatePayload(int playerID, int roomID, boolean isMicOn) {
        this.playerID = playerID;
        this.roomID = roomID;
        this.isMicOn = isMicOn;
    }

    public int getPlayerID() { return playerID; }
    public int getRoomID() { return roomID; }
    public boolean isMicOn() { return isMicOn; }
}