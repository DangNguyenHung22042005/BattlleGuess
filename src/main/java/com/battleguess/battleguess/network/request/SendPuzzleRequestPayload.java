package com.battleguess.battleguess.network.request;

import com.battleguess.battleguess.network.Payload;

public class SendPuzzleRequestPayload implements Payload {
    private int playerID;
    private int roomID;
    private byte[] imageData;
    private String answer;

    public SendPuzzleRequestPayload(int playerID, int roomID, byte[] imageData, String answer) {
        this.playerID = playerID;
        this.roomID = roomID;
        this.imageData = imageData;
        this.answer = answer;
    }

    public int getPlayerID() { return playerID; }
    public int getRoomID() { return roomID; }
    public byte[] getImageData() { return imageData; }
    public String getAnswer() { return answer; }
}