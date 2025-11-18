package com.battleguess.battleguess.network.response;

import com.battleguess.battleguess.network.Payload;

public class AudioFramePayload implements Payload {
    private int senderID;
    private byte[] audioData;

    public AudioFramePayload(int senderID, byte[] audioData) {
        this.senderID = senderID;
        this.audioData = audioData;
    }

    public int getSenderID() {
        return senderID;
    }

    public byte[] getAudioData() {
        return audioData;
    }
}
