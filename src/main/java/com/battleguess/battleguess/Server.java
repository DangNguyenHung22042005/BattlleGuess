package com.battleguess.battleguess;

import com.battleguess.battleguess.enum_to_manage_string.MessageType;
import com.battleguess.battleguess.object_serializable.GameMessage;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class Server implements Runnable {
    private ServerSocket serverSocket;
    private Thread thread;
    private boolean running;
    private int port;
    private HashMap<String, Room> rooms;
    private HashMap<ObjectOutputStream, String> clientNames;

    public Server(int port) throws IOException {
        this.port = port;
        this.running = false;
        this.rooms = new HashMap<>();
        this.clientNames = new HashMap<>();
        createServerSocket();
    }

    private void createServerSocket() throws IOException {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server created on port " + port);
        } catch (IOException e) {
            throw new IOException("Port " + port + " is already in use or invalid.");
        }
    }

    public void startServer() {
        if (!running) {
            running = true;
            try {
                if (serverSocket == null || serverSocket.isClosed()) {
                    createServerSocket();
                }
                thread = new Thread(this);
                thread.start();
                System.out.println("Server started on port " + port);
            } catch (IOException e) {
                running = false;
                throw new RuntimeException("Failed to start server on port " + port + ": " + e.getMessage());
            }
        }
    }

    public void stopServer() {
        if (running) {
            running = false;
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
                if (thread != null) {
                    thread.interrupt();
                }
                System.out.println("Server stopped on port " + port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                if (clientSocket != null) {
                    System.out.println("Client connected on port " + port);
                    new Thread(() -> handleClient(clientSocket)).start();
                }
            } catch (IOException e) {
                if (!running) {

                    break;
                }
                e.printStackTrace();
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try {
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            while (running) {
                GameMessage message = (GameMessage) in.readObject();
                MessageType type = message.getType();
                switch (type) {
                    case MessageType.CREATE_ROOM:
                        Room room = new Room(message.getRoomName(), message.getPlayerName());
                        rooms.put(room.getId(), room);
                        room.addClient(out);
                        clientNames.put(out, message.getPlayerName());
                        out.writeObject(new GameMessage(MessageType.ROOM_CREATED, message.getPlayerName(), room.getId(), room.getName(), null, null));
                        break;
                    case MessageType.JOIN_ROOM:
                        Room joinRoom = rooms.get(message.getRoomId());
                        if (joinRoom != null) {
                            joinRoom.addClient(out);
                            clientNames.put(out, message.getPlayerName());
                            out.writeObject(new GameMessage(MessageType.JOINED_ROOM, message.getPlayerName(), joinRoom.getId(), joinRoom.getName(), null, null));
                        } else {
                            out.writeObject(new GameMessage(MessageType.ERROR, message.getPlayerName(), null, null, null, "Room not found"));
                        }
                        break;
                    case MessageType.REQUEST_KEY:
                        Room keyRoom = rooms.get(message.getRoomId());
                        if (keyRoom != null) {
                            keyRoom.setKeyHolder(message.getPlayerName());
                            broadcastToRoom(keyRoom, new GameMessage(MessageType.KEY_CHANGED, message.getPlayerName(), keyRoom.getId(), keyRoom.getName(), null, null));
                        }
                        break;
                    case MessageType.SEND_PUZZLE:
                        Room puzzleRoom = rooms.get(message.getRoomId());
                        if (puzzleRoom != null && message.getPlayerName().equals(puzzleRoom.getKeyHolder())) {
                            puzzleRoom.setCorrectAnswer(message.getAnswer());
                            broadcastToRoom(puzzleRoom, new GameMessage(MessageType.PUZZLE_RECEIVED, message.getPlayerName(), puzzleRoom.getId(), puzzleRoom.getName(), message.getImageData(), null));
                        }
                        break;
                    case MessageType.SEND_ANSWER:
                        Room answerRoom = rooms.get(message.getRoomId());
                        if (answerRoom != null && !message.getPlayerName().equals(answerRoom.getKeyHolder())) {
                            if (message.getAnswer().equalsIgnoreCase(answerRoom.getCorrectAnswer())) {
                                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                                System.out.println("Player " + message.getPlayerName() + " in room " + answerRoom.getId() + " answered correctly: " + message.getAnswer() + " at " + timestamp);
                                broadcastToRoom(answerRoom, new GameMessage(MessageType.ANSWER_CORRECT, message.getPlayerName(), answerRoom.getId(), answerRoom.getName(), null, message.getAnswer()));
                            }
                        }
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (running) e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcastToRoom(Room room, GameMessage message) {
        for (ObjectOutputStream client : room.getClients()) {
            try {
                client.writeObject(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}