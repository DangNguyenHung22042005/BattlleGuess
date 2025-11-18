package com.battleguess.battleguess.network.response;

import com.battleguess.battleguess.model.RoomInfo;
import com.battleguess.battleguess.network.Payload;

public class JoinRequestResponsePayload implements Payload {
    private int targetPlayerID;
    private RoomInfo roomInfo;
    private boolean approved;
    private String joinerName;

    public JoinRequestResponsePayload(int targetPlayerID, RoomInfo roomInfo, boolean approved, String joinerName) { // <-- SỬA CONSTRUCTOR
        this.targetPlayerID = targetPlayerID;
        this.roomInfo = roomInfo;
        this.approved = approved;
        this.joinerName = joinerName;
    }

    public int getTargetPlayerID() { return targetPlayerID; }
    public RoomInfo getRoomInFo() { return roomInfo; }
    public boolean isApproved() { return approved; }
    public String getJoinerName() { return joinerName; }
}
