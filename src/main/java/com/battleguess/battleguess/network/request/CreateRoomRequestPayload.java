package com.battleguess.battleguess.network.request;

import com.battleguess.battleguess.network.Payload;

public class CreateRoomRequestPayload implements Payload {
    private int playerID;
    private String roomName;
    private String roomCode;

    public CreateRoomRequestPayload(int playerID, String roomName, String roomCode) {
        this.playerID = playerID;
        this.roomName = roomName;
        this.roomCode = roomCode;
    }

    public int getPlayerID() { return playerID; }
    public String getRoomName() { return roomName; }
    public String getRoomCode() { return roomCode; }
}
