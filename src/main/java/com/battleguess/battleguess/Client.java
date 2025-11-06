package com.battleguess.battleguess;

import com.battleguess.battleguess.object_serializable.GameMessage;
import com.battleguess.battleguess.controller.ClientController;
import javafx.application.Platform;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ClientController controller;
    private String playerName;

    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor();

    public Client(String host, int port, String playerName, ClientController controller) {
        this.playerName = playerName;
        this.controller = controller;
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            if (socket != null) {
                System.out.println("Connect successfully!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        startListening();
    }

    public void sendMessage(GameMessage message) {
        sendExecutor.submit(() -> {
            try {
                synchronized (out) {
                    out.writeObject(message);
                    out.flush();
                    out.reset(); // quan trọng nếu gửi nhiều object tương tự để tránh memory leak
                }
            } catch (IOException e) {
                e.printStackTrace();
                Platform.runLater(() -> controller.showAlert("Error", "Connection error: " + e.getMessage()));
            }
        });
    }

    private void startListening() {
        new Thread(() -> {
            try {
                while (true) {
                    GameMessage message = (GameMessage) in.readObject();
                    Platform.runLater(() -> controller.handleServerMessage(message));
                }
            } catch (EOFException e) {
                System.out.println("Server disconnected.");
                Platform.runLater(() -> controller.showAlert("Error", "Lost connection to server."));
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                Platform.runLater(() -> controller.showAlert("Error", "Connection error: " + e.getMessage()));
            } finally {
                disconnect();
            }
        }).start();
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
            sendExecutor.shutdownNow();
            System.out.println("Client disconnected!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
