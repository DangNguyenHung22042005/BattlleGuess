package com.battleguess.battleguess.network.request;

import com.battleguess.battleguess.network.Payload;

public class SendChatMessageRequestPayload implements Payload {
    private int playerID;
    private int roomID;
    private String messageContent;

    public SendChatMessageRequestPayload(int playerID, int roomID, String messageContent) {
        this.playerID = playerID;
        this.roomID = roomID;
        this.messageContent = messageContent;
    }

    public int getPlayerID() { return playerID; }
    public int getRoomID() { return roomID; }
    public String getMessageContent() { return messageContent; }
}