package com.battleguess.battleguess.network.response;

import com.battleguess.battleguess.model.PlayerState;
import com.battleguess.battleguess.model.RoomInfo;
import com.battleguess.battleguess.network.Payload;
import java.util.List;

public class RoomJoinSuccessPayload implements Payload {
    private RoomInfo roomInfo;
    private List<PlayerState> playerList;

    public RoomJoinSuccessPayload(RoomInfo roomInfo, List<PlayerState> playerList) {
        this.roomInfo = roomInfo;
        this.playerList = playerList;
    }

    public RoomInfo getRoomInfo() { return roomInfo; }
    public List<PlayerState> getPlayerList() { return playerList; }
}
