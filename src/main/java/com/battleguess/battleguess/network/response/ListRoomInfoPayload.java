package com.battleguess.battleguess.network.response;

import com.battleguess.battleguess.model.RoomInfo;
import com.battleguess.battleguess.network.Payload;
import java.util.List;

public class ListRoomInfoPayload implements Payload {
    private List<RoomInfo> listRoomInfo;

    public ListRoomInfoPayload(List<RoomInfo> listRoomInfo) {
        this.listRoomInfo = listRoomInfo;
    }

    public List<RoomInfo> getListRoomInfo() { return listRoomInfo; }
}
