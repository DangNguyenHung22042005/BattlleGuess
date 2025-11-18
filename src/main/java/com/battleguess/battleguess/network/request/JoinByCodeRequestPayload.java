package com.battleguess.battleguess.network.request;

import com.battleguess.battleguess.network.Payload;

public class JoinByCodeRequestPayload implements Payload {
    private int playerID;
    private String playerName;
    private String roomCode;

    public JoinByCodeRequestPayload(int playerID, String playerName, String roomCode) {
        this.playerID = playerID;
        this.playerName = playerName;
        this.roomCode = roomCode;
    }

    public int getPlayerID() { return playerID; }
    public String getPlayerName() { return playerName; }
    public String getRoomCode() { return roomCode; }
}
