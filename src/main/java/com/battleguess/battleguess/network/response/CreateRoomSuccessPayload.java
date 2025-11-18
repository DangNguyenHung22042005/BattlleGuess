package com.battleguess.battleguess.network.response;

import com.battleguess.battleguess.model.RoomInfo;
import com.battleguess.battleguess.network.Payload;

public class CreateRoomSuccessPayload implements Payload {
    private RoomInfo roomInfo;

    public CreateRoomSuccessPayload(RoomInfo roomInfo) {
        this.roomInfo = roomInfo;
    }

    public RoomInfo getRoomInfo() { return roomInfo; }
}
