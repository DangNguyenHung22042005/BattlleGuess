package com.battleguess.battleguess.controller;

import com.battleguess.battleguess.Server;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.io.IOException;

public class ServerController {
    @FXML private TextField portField;
    @FXML private ListView<Server> serverList;

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
                            Region spacer = new Region();
                            HBox.setHgrow(spacer, Priority.ALWAYS);

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

                            Button manageButton = new Button("Quản lý");
                            manageButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                            // Chỉ cho phép quản lý khi server đang chạy
                            manageButton.disableProperty().bind(toggleButton.textProperty().isEqualTo("Start"));

                            manageButton.setOnAction(event -> openAdminDashboard(server));

                            hbox.getChildren().addAll(portLabel, spacer, manageButton, toggleButton);
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

    private void openAdminDashboard(Server server) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/battleguess/battleguess/view/admin-dashboard.fxml"));
            Parent root = loader.load();

            // Truyền instance Server đang chạy sang Admin Controller
            AdminDashboardController adminController = loader.getController();
            adminController.setServerInstance(server);

            Stage stage = new Stage();
            stage.setTitle("Admin Dashboard - Port " + server.getPort());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể mở giao diện quản trị.");
        }
    }
}
