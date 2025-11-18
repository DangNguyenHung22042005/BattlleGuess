package com.battleguess.battleguess.network.request;

import com.battleguess.battleguess.network.Payload;

public class RegisterUdpPayload implements Payload {
    private int playerID;
    private int udpPort; // Port mà Client mở để *nhận*

    public RegisterUdpPayload(int playerID, int udpPort) {
        this.playerID = playerID; this.udpPort = udpPort;
    }

    public int getPlayerID() { return playerID; }
    public int getUdpPort() { return udpPort; }
}