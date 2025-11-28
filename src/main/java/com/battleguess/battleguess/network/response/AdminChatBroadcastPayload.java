package com.battleguess.battleguess.network.response;

import com.battleguess.battleguess.network.Payload;

public class AdminChatBroadcastPayload implements Payload {
    private String messageContent;

    public AdminChatBroadcastPayload(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getMessageContent() {
        return messageContent;
    }
}