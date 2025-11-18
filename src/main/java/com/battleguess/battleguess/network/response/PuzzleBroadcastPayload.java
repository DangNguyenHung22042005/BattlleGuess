package com.battleguess.battleguess.network.response;

import com.battleguess.battleguess.network.Payload;

public class PuzzleBroadcastPayload implements Payload {
    private final byte[] imageData;

    public PuzzleBroadcastPayload(byte[] imageData) {
        this.imageData = imageData;
    }

    public byte[] getImageData() { return imageData; }
}