package com.battleguess.battleguess.network.response;

import com.battleguess.battleguess.model.PlayerState;
import com.battleguess.battleguess.model.RoomInfo;
import com.battleguess.battleguess.network.Payload;
import java.util.List;

public class RoomStateUpdatePayload implements Payload {
    private RoomInfo roomInfo;
    private List<PlayerState> playerStates;

    public RoomStateUpdatePayload(RoomInfo roomInfo, List<PlayerState> playerStates) {
        this.roomInfo = roomInfo;
        this.playerStates = playerStates;
    }

    public RoomInfo getRoomInfo() { return roomInfo; }
    public List<PlayerState> getPlayerStates() { return playerStates; }
}
