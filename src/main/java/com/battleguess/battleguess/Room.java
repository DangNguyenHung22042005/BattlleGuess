package com.battleguess.battleguess;

import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class Room {
    private final String id;
    private final String name;
    private String keyHolder;
    private final ArrayList<ObjectOutputStream> clients;
    private String correctAnswer;

    public Room(String name, String keyHolder) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.keyHolder = keyHolder;
        this.clients = new ArrayList<>();
        this.correctAnswer = null;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getKeyHolder() { return keyHolder; }
    public ArrayList<ObjectOutputStream> getClients() { return clients; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String answer) { this.correctAnswer = answer; }
    public void setKeyHolder(String keyHolder) { this.keyHolder = keyHolder; }
    public void addClient(ObjectOutputStream out) { clients.add(out); }
    public void removeClient(ObjectOutputStream out) { clients.remove(out); }
}
