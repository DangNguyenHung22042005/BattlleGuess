package com.battleguess.battleguess.object_serializable;

import java.io.Serializable;

public class CanvasImageData implements Serializable {
    private byte[] imageBytes;

    public CanvasImageData(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }
}