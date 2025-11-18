package com.battleguess.battleguess.network.response;

import com.battleguess.battleguess.network.Payload;

/**
 * Gói tin này chứa một khung hình video (đã nén).
 * Server sẽ "phản xạ" (reflect) gói tin này đến các client khác
 * (bản thân nó cũng được dùng nội bộ trong Client để hợp nhất luồng UDP).
 */
public class VideoFramePayload implements Payload {
    private int playerID;     // ID của người gửi khung hình
    private byte[] frameData; // Dữ liệu ảnh JPEG nén

    public VideoFramePayload(int playerID, byte[] frameData) {
        this.playerID = playerID;
        this.frameData = frameData;
    }

    public int getPlayerID() {
        return playerID;
    }

    public byte[] getFrameData() {
        return frameData;
    }
}
