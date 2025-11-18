package com.battleguess.battleguess.network.response;

import com.battleguess.battleguess.network.Payload;

public class GenericResponsePayload implements Payload {
    private String message;

    public GenericResponsePayload(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}