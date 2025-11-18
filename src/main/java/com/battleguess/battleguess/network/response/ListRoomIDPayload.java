package com.battleguess.battleguess.network.response;

import com.battleguess.battleguess.network.Payload;

import java.util.List;

public class ListRoomIDPayload implements Payload {
    private List<Integer> listRoomIDs;

    public ListRoomIDPayload(List<Integer> listRoomIDs) {
        this.listRoomIDs = listRoomIDs;
    }

    public List<Integer> getListRoomIDs() { return listRoomIDs; }
}
