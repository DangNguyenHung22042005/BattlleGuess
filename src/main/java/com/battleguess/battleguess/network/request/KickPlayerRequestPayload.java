package com.battleguess.battleguess.network.request;

import com.battleguess.battleguess.network.Payload;

public class KickPlayerRequestPayload implements Payload {
    private int playerID;
    private int currentRoomID;
    private int targetPlayerToKickID;

    public KickPlayerRequestPayload(int playerID, int currentRoomID, int targetPlayerToKickID) {
        this.playerID = playerID;
        this.currentRoomID = currentRoomID;
        this.targetPlayerToKickID = targetPlayerToKickID;
    }

    public int getPlayerID() { return playerID; }
    public int getCurrentRoomID() { return currentRoomID; }
    public int getTargetPlayerToKickID() { return targetPlayerToKickID; }
}
