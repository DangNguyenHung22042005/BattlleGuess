package com.battleguess.battleguess.model;

import java.io.Serializable;

public class RoomInfo implements Serializable {
    private final int roomID;
    private final String roomName;
    private final String roomCode;

    public RoomInfo(int roomID, String roomName, String roomCode) {
        this.roomID = roomID;
        this.roomName = roomName;
        this.roomCode = roomCode;
    }

    public int getRoomID() {
        return roomID;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getRoomCode() {
        return roomCode;
    }

    @Override
    public String toString() {
        return roomName + " (Code: " + roomCode + ")";
    }
}