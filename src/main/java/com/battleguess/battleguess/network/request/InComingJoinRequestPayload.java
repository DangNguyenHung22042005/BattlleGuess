package com.battleguess.battleguess.network.request;

import com.battleguess.battleguess.model.RoomInfo;
import com.battleguess.battleguess.network.Payload;

public class InComingJoinRequestPayload implements Payload {
    private int joinerID;
    private String joinerName;
    private RoomInfo roomInfo;

    public InComingJoinRequestPayload(int joinerID, String joinerName, RoomInfo roomInfo) {
        this.joinerID = joinerID;
        this.joinerName = joinerName;
        this.roomInfo = roomInfo;
    }

    public int getJoinerID() { return joinerID; }
    public String getJoinerName() { return joinerName; }
    public RoomInfo getRoomInfo() { return roomInfo; }
}
