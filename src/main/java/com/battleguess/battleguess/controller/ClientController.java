package com.battleguess.battleguess.controller;

import com.battleguess.battleguess.Client;
import com.battleguess.battleguess.canvas.DrawingPane;
import com.battleguess.battleguess.enum_to_manage_string.CanvasToolType;
import com.battleguess.battleguess.enum_to_manage_string.MessageType;
import com.battleguess.battleguess.object_serializable.CanvasImageData;
import com.battleguess.battleguess.object_serializable.GameMessage;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ClientController {
    @FXML
    private VBox canvasContainer, gamePane, createRoomPane, joinRoomPane;
    @FXML
    private TextField nameField, portField, roomNameField, roomIdField, answerField, puzzleAnswerField;
    @FXML
    private TextField roomLabel;
    @FXML
    private HBox roomInfoPane, answerPane, puzzlePane;
    @FXML
    private DrawingPane drawingPane;

    private CanvasController canvasController;
    private Client client;
    private String playerName;
    private String roomId;
    private boolean isKeyHolder;

    @FXML
    private void initialize() {
        loadSimpleCanvasView();
        drawingPane.setCurrentTool(CanvasToolType.PENCIL);
        drawingPane.setCurrentStrokeSize(1.0);
        drawingPane.setCurrentColor(Color.BLACK);
        drawingPane.setEraserSize(5.0);
    }

    @FXML
    private void connectToServer() {
        String name = nameField.getText().trim();
        String portText = portField.getText().trim();
        if (name.isEmpty()) {
            showAlert("Invalid Name", "Please enter a name.");
            return;
        }
        try {
            int port = Integer.parseInt(portText);
            if (port < 1024 || port > 65535) {
                showAlert("Invalid Port", "Port must be between 1024 and 65535.");
                return;
            }
            playerName = name;
            client = new Client("localhost", port, playerName, this);
            showGamePane();
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter a valid port number.");
        }
    }

    @FXML
    private void showCreateRoom() {
        createRoomPane.setVisible(true);
        createRoomPane.setManaged(true);
        gamePane.setVisible(false);
        gamePane.setManaged(false);
    }

    @FXML
    private void showJoinRoom() {
        joinRoomPane.setVisible(true);
        joinRoomPane.setManaged(true);
        gamePane.setVisible(false);
        gamePane.setManaged(false);
    }

    @FXML
    private void showGamePane() {
        gamePane.setVisible(true);
        gamePane.setManaged(true);
        createRoomPane.setVisible(false);
        createRoomPane.setManaged(false);
        joinRoomPane.setVisible(false);
        joinRoomPane.setManaged(false);
    }

    @FXML
    private void createRoom() {
        String roomName = roomNameField.getText().trim();
        if (roomName.isEmpty()) {
            showAlert("Invalid Room Name", "Please enter a room name.");
            return;
        }
        client.sendMessage(new GameMessage(MessageType.CREATE_ROOM, playerName, null, roomName, null, null));
        showGamePane();
    }

    @FXML
    private void joinRoom() {
        String roomId = roomIdField.getText().trim();
        if (roomId.isEmpty()) {
            showAlert("Invalid Room ID", "Please enter a room ID.");
            return;
        }
        client.sendMessage(new GameMessage(MessageType.JOIN_ROOM, playerName, roomId, null, null, null));
        showGamePane();
    }

    @FXML
    private void requestKey() {
        if (roomId != null) {
            client.sendMessage(new GameMessage(MessageType.REQUEST_KEY, playerName, roomId, null, null, null));
        }
    }

    @FXML
    private void sendPuzzle() throws IOException {
        if (isKeyHolder && roomId != null) {
            String answer = puzzleAnswerField.getText().trim();
            if (answer.isEmpty()) {
                showAlert("Invalid Answer", "Please enter a puzzle answer.");
                return;
            }
            WritableImage img = new WritableImage((int)drawingPane.getWidth(), (int)drawingPane.getHeight());
            drawingPane.snapshot(null, img);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", baos);
            byte[] imageBytes = baos.toByteArray();

            CanvasImageData imageData = new CanvasImageData(imageBytes);

            GameMessage message = new GameMessage(MessageType.SEND_PUZZLE, playerName, roomId, null, imageData, answer);
            client.sendMessage(message);
            puzzleAnswerField.clear();
        }
    }

    @FXML
    private void sendAnswer() {
        if (!isKeyHolder && roomId != null) {
            String answer = answerField.getText().trim();
            if (answer.isEmpty()) {
                showAlert("Invalid Answer", "Please enter an answer.");
                return;
            }
            client.sendMessage(new GameMessage(MessageType.SEND_ANSWER, playerName, roomId, null, null, answer));
            answerField.clear();
        }
    }

    public void handleServerMessage(GameMessage message) {
        switch (message.getType()) {
            case MessageType.ROOM_CREATED:
                roomId = message.getRoomId();
                roomLabel.setText("Room: " + message.getRoomName() + " (ID: " + roomId + ")");
                roomInfoPane.setVisible(true);
                roomInfoPane.setManaged(true);
                isKeyHolder = true;
                loadFullCanvasView();
                puzzlePane.setVisible(true);
                puzzlePane.setManaged(true);
                answerPane.setVisible(false);
                answerPane.setManaged(false);
                break;
            case MessageType.JOINED_ROOM:
                roomId = message.getRoomId();
                roomLabel.setText("Room: " + message.getRoomName() + " (ID: " + roomId + ")");
                roomInfoPane.setVisible(true);
                roomInfoPane.setManaged(true);
                isKeyHolder = false;
                loadSimpleCanvasView();
                answerPane.setVisible(true);
                answerPane.setManaged(true);
                puzzlePane.setVisible(false);
                puzzlePane.setManaged(false);
                break;
            case MessageType.KEY_CHANGED:
                isKeyHolder = playerName.equals(message.getPlayerName());
                if (isKeyHolder) {
                    loadFullCanvasView();
                } else {
                    loadSimpleCanvasView();
                }
                puzzlePane.setVisible(isKeyHolder);
                puzzlePane.setManaged(isKeyHolder);
                answerPane.setVisible(!isKeyHolder);
                answerPane.setManaged(!isKeyHolder);
                break;
            case MessageType.PUZZLE_RECEIVED:
                CanvasImageData imageData = message.getImageData();
                if (imageData != null) {
                    byte[] bytes = imageData.getImageBytes();
                    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                    Image image = new Image(bais);

                    Platform.runLater(() -> {
                        GraphicsContext gc = drawingPane.getGraphicsContext2D();
                        gc.drawImage(image, 0, 0);
                    });
                }
                break;
            case MessageType.ANSWER_CORRECT:
                showAlert("Correct Answer", "Player " + message.getPlayerName() + " answered correctly: " + message.getAnswer());
                if (drawingPane != null) {
                    drawingPane.clearDrawing();
                }
                break;
            case MessageType.ERROR:
                showAlert("Error", message.getAnswer());
                break;
        }
    }

    private void loadFullCanvasView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/battleguess/battleguess/view/canvas-view.fxml"));
            Parent canvasView = loader.load();
            canvasController = loader.getController();
            drawingPane = canvasController.drawingPane;

            canvasContainer.getChildren().clear();
            canvasContainer.getChildren().add(canvasView);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSimpleCanvasView() {
        drawingPane = new DrawingPane();
        drawingPane.setDrawingEnabled(false);
        canvasContainer.getChildren().clear();
        canvasContainer.getChildren().add(drawingPane);
    }

    public void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
