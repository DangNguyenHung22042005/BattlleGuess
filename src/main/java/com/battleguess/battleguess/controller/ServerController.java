package com.battleguess.battleguess.controller;

import com.battleguess.battleguess.Server;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import java.io.IOException;

public class ServerController {
    @FXML
    private TextField portField;
    @FXML
    private ListView<Server> serverList;

    private ObservableList<Server> servers;

    @FXML
    private void initialize() {
        servers = FXCollections.observableArrayList();
        serverList.setItems(servers);

        // Tùy chỉnh hiển thị mỗi item trong ListView
        serverList.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Server> call(ListView<Server> param) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(Server server, boolean empty) {
                        super.updateItem(server, empty);
                        if (empty || server == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            HBox hbox = new HBox(10);
                            Label portLabel = new Label("Port: " + server.getPort());
                            Button toggleButton = new Button(server.isRunning() ? "Stop" : "Start");
                            toggleButton.getStyleClass().add("button");
                            if (server.isRunning()) {
                                toggleButton.getStyleClass().add("active-tool");
                            }
                            toggleButton.setOnAction(event -> {
                                if (server.isRunning()) {
                                    server.stopServer();
                                    toggleButton.setText("Start");
                                    toggleButton.getStyleClass().remove("active-tool");
                                } else {
                                    server.startServer();
                                    toggleButton.setText("Stop");
                                    toggleButton.getStyleClass().add("active-tool");
                                }
                            });
                            hbox.getChildren().addAll(portLabel, toggleButton);
                            setGraphic(hbox);
                        }
                    }
                };
            }
        });
    }

    @FXML
    private void createServer() {
        String portText = portField.getText().trim();
        try {
            int port = Integer.parseInt(portText);
            if (port < 1024 || port > 65535) {
                showAlert("Invalid Port", "Port must be between 1024 and 65535.");
                return;
            }
            Server server = new Server(port);
            servers.add(server);
            portField.clear();
        } catch (NumberFormatException e) {
            showAlert("Invalid Input", "Please enter a valid port number.");
        } catch (IOException e) {
            showAlert("Port Error", e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
