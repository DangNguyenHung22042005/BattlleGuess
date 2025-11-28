package com.battleguess.battleguess.model;

import com.battleguess.battleguess.network.Packet;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RoomSession {
    private RoomInfo roomInfo;
    private int ownerPlayerID;
    // Key: PlayerID, Value: Trạng thái (Online/Offline, Score, Stream)
    private Map<Integer, PlayerState> players = new ConcurrentHashMap<>();
    // Key: PlayerID, Value: Stream (chỉ khi Online)
    private Map<Integer, ObjectOutputStream> clientStreams = new ConcurrentHashMap<>();
    private String correctAnswer;
    private Set<Integer> playersWithCameraOn = ConcurrentHashMap.newKeySet();
    private Set<Integer> playersWithMicOn = ConcurrentHashMap.newKeySet();
    private Map<Integer, SocketAddress> playerUdpAddresses = new ConcurrentHashMap<>();

    public RoomSession(RoomInfo roomInfo, int ownerPlayerID, List<PlayerState> initialMembers) {
        this.roomInfo = roomInfo;
        this.ownerPlayerID = ownerPlayerID;
        for (PlayerState p : initialMembers) {
            this.players.put(p.getPlayerID(), p);
        }
    }

    public RoomInfo getRoomInfo() {
        return roomInfo;
    }

    public int getOwnerPlayerID() {
        return ownerPlayerID;
    }

    public List<PlayerState> getPlayerStates() {
        return List.copyOf(players.values());
    }

    public void playerJoin(int playerID, ObjectOutputStream stream) {
        PlayerState state = players.get(playerID);
        if (state != null) {
            // Cập nhật trạng thái
            PlayerState newState = new PlayerState(playerID, state.getUsername(), state.getScore(), state.isOwner(), true);
            players.put(playerID, newState); // Cập nhật trạng thái "Online"
            clientStreams.put(playerID, stream);
        }
    }

    public void playerJoin(int playerID, String username, int score, boolean isOwner, ObjectOutputStream stream) {
        PlayerState newState = new PlayerState(playerID, username, score, isOwner, true);
        players.put(playerID, newState); // Thêm mới vào map
        clientStreams.put(playerID, stream); // Thêm stream
    }

    public void playerLeave(int playerID) {
        PlayerState state = players.get(playerID);
        if (state != null) {
            PlayerState newState = new PlayerState(playerID, state.getUsername(), state.getScore(), state.isOwner(), false);
            players.put(playerID, newState); // Cập nhật trạng thái "Offline"
        }
        clientStreams.remove(playerID); // Xóa stream
        playerUdpAddresses.remove(playerID);
    }

    public PlayerState playerKick(int playerID) {
        clientStreams.remove(playerID);
        playerUdpAddresses.remove(playerID);
        return players.remove(playerID); // Xóa hẳn khỏi session
    }

    public void broadcast(Packet packet) {
        for (ObjectOutputStream stream : clientStreams.values()) {
            try {
                stream.writeObject(packet);
                stream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ObjectOutputStream getStream(int playerID) {
        return clientStreams.get(playerID);
    }

    public void setCorrectAnswer(String answer) {
        this.correctAnswer = (answer != null) ? answer.toLowerCase() : null;
    }

    public String getCorrectAnswer() {
        return this.correctAnswer;
    }

    public void clearCorrectAnswer() {
        this.correctAnswer = null;
    }

    public void updatePlayerScore(int playerID, int newScore) {
        PlayerState oldState = players.get(playerID);
        if (oldState != null) {
            PlayerState newState = new PlayerState(
                    playerID,
                    oldState.getUsername(),
                    newScore,
                    oldState.isOwner(),
                    oldState.isOnline()
            );
            players.put(playerID, newState);
        }
    }

    public void broadcastToGuessers(Packet packet) {
        for (Map.Entry<Integer, ObjectOutputStream> entry : clientStreams.entrySet()) {
            if (entry.getKey() != ownerPlayerID) { // Bỏ qua chủ phòng
                try {
                    entry.getValue().writeObject(packet);
                    entry.getValue().flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getPlayerName(int playerID) {
        PlayerState state = players.get(playerID);
        return (state != null) ? state.getUsername() : "Unknown";
    }

    public void registerUdpAddress(int playerID, SocketAddress address) {
        playerUdpAddresses.put(playerID, address);
    }

    public void unregisterUdpAddress(int playerID) {
        playerUdpAddresses.remove(playerID);
    }

    public Map<Integer, SocketAddress> getUdpAddresses() {
        return playerUdpAddresses;
    }

    public void setPlayerCameraStatus(int playerID, boolean isCameraOn) {
        if (isCameraOn) { playersWithCameraOn.add(playerID); }
        else { playersWithCameraOn.remove(playerID); }
    }

    public void setPlayerMicStatus(int playerID, boolean isMicOn) {
        if (isMicOn) {
            playersWithMicOn.add(playerID);
        } else {
            playersWithMicOn.remove(playerID);
        }
    }

    public boolean hasPlayer(int playerID) {
        return players.containsKey(playerID);
    }
}
