package com.battleguess.battleguess.network;

import com.battleguess.battleguess.enum_to_manage_string.MessageType;
import java.io.Serializable;

public class Packet implements Serializable {
    private MessageType type;
    private Payload data;

    public Packet(MessageType type, Payload data) {
        this.type = type;
        this.data = data;
    }

    public MessageType getType() {
        return type;
    }

    public Payload getData() {
        return data;
    }

    public <T extends Payload> T getData(Class<T> classType) {
        return classType.cast(data);
    }
}