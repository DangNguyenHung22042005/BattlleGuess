package com.battleguess.battleguess.object_serializable;

import com.battleguess.battleguess.enum_to_manage_string.MessageType;

import java.io.Serializable;

public class GameMessage implements Serializable {
    private MessageType type;
    private String playerName;
    private String roomId;
    private String roomName;
    private CanvasImageData imageData;
    private String answer;

    public GameMessage(MessageType type, String playerName, String roomId, String roomName, CanvasImageData imageData, String answer) {
        this.type = type;
        this.playerName = playerName;
        this.roomId = roomId;
        this.roomName = roomName;
        this.imageData = imageData;
        this.answer = answer;
    }

    public CanvasImageData getImageData() {
        return imageData;
    }
    public MessageType getType() { return type; }
    public String getPlayerName() { return playerName; }
    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getAnswer() { return answer; }
}