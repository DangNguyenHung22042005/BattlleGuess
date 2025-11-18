package com.battleguess.battleguess.network.request;

import com.battleguess.battleguess.network.Payload;

public class PlayerIDAndRoomIDPayload implements Payload {
    private int playerID;
    private int roomID;

    public PlayerIDAndRoomIDPayload(int playerID, int roomID) {
        this.playerID = playerID;
        this.roomID = roomID;
    }

    public int getPlayerID() { return playerID; }

    public int getRoomID() { return roomID; }
}
