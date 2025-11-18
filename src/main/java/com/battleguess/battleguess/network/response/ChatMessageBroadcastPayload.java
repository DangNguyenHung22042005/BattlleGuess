package com.battleguess.battleguess.network.response;

import com.battleguess.battleguess.network.Payload;

public class ChatMessageBroadcastPayload implements Payload {
    private int senderID;
    private String senderName;
    private String messageContent;

    public ChatMessageBroadcastPayload(int senderID, String senderName, String messageContent) {
        this.senderID = senderID;
        this.senderName = senderName;
        this.messageContent = messageContent;
    }

    public int getSenderID() { return senderID; }
    public String getSenderName() { return senderName; }
    public String getMessageContent() { return messageContent; }
}