package com.battleguess.battleguess.network.request;

import com.battleguess.battleguess.network.Payload;

public class RoomIDPayload implements Payload {
    private int roomID;

    public RoomIDPayload(int roomID) {
        this.roomID = roomID;
    }

    public int getRoomID() { return roomID; }
}
