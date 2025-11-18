package com.battleguess.battleguess.network.response;

import com.battleguess.battleguess.network.Payload;
import java.io.Serializable;
import java.net.SocketAddress;
import java.util.Map;

public class PlayerUdpListPayload implements Payload, Serializable {
    // Map<PlayerID, Địa chỉ UDP (IP:Port) của họ>
    private Map<Integer, SocketAddress> udpAddresses;

    public PlayerUdpListPayload(Map<Integer, SocketAddress> udpAddresses) {
        this.udpAddresses = udpAddresses;
    }

    public Map<Integer, SocketAddress> getUdpAddresses() { return udpAddresses; }
}