package com.battleguess.battleguess.network.request;

import com.battleguess.battleguess.network.Payload;

public class SendGuessRequestPayload implements Payload {
    private int playerID;
    private String playerName;
    private int roomID;
    private String guess;

    public SendGuessRequestPayload(int playerID, String playerName, int roomID, String guess) {
        this.playerID = playerID;
        this.playerName = playerName;
        this.roomID = roomID;
        this.guess = guess;
    }

    public int getPlayerID() { return playerID; }
    public String getPlayerName() { return playerName; }
    public int getRoomID() { return roomID; }
    public String getGuess() { return guess; }
}