package com.battleguess.battleguess.controller;

import com.battleguess.battleguess.Server;
import com.battleguess.battleguess.database.DatabaseManager;
import com.battleguess.battleguess.model.AdminPlayerRow;
import com.battleguess.battleguess.model.AdminRoomRow;
import com.battleguess.battleguess.model.LogEntry;
import com.battleguess.battleguess.model.PlayerState;
import com.battleguess.battleguess.service.ServerLogger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AdminDashboardController {
    @FXML private Label lblHeader;
    @FXML private Button btnReload;

    @FXML private Button btnManagePlayers;
    @FXML private TableView<AdminPlayerRow> playerTable;
    @FXML private TableColumn<AdminPlayerRow, Number> colID;
    @FXML private TableColumn<AdminPlayerRow, String> colName;
    @FXML private TableColumn<AdminPlayerRow, Number> colScore;
    @FXML private TableColumn<AdminPlayerRow, String> colStatus;
    @FXML private TableColumn<AdminPlayerRow, String> colRoom;
    @FXML private TableColumn<AdminPlayerRow, Void> colAction;

    @FXML private Button btnManageRooms;
    @FXML private TableView<AdminRoomRow> roomTable;
    @FXML private TableColumn<AdminRoomRow, Number> colRoomID;
    @FXML private TableColumn<AdminRoomRow, String> colRoomName;
    @FXML private TableColumn<AdminRoomRow, String> colRoomCode;
    @FXML private TableColumn<AdminRoomRow, String> colOwner;
    @FXML private TableColumn<AdminRoomRow, String> colRoomStatus;
    @FXML private TableColumn<AdminRoomRow, Number> colMembers;
    @FXML private TableColumn<AdminRoomRow, Void> colRoomAction;

    @FXML private Button btnSystemLogs;
    @FXML private Button btnClearLogs;
    @FXML private ListView<LogEntry> logListView;

    private Server serverInstance;
    private DatabaseManager db;
    private ObservableList<AdminPlayerRow> playerList = FXCollections.observableArrayList();
    private ObservableList<AdminRoomRow> roomList = FXCollections.observableArrayList();

    // 0:Player, 1:Room, 2:Log
    private int currentView = 0;

    public void setServerInstance(Server server) {
        this.serverInstance = server;
        this.db = server.getDatabaseManager();
        loadPlayerData();
    }

    @FXML
    private void initialize() {
        colID.setCellValueFactory(cell -> cell.getValue().idProperty());
        colName.setCellValueFactory(cell -> cell.getValue().usernameProperty());
        colScore.setCellValueFactory(cell -> cell.getValue().scoreProperty());
        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
        colRoom.setCellValueFactory(cell -> cell.getValue().currentRoomProperty());
        colStatus.setCellFactory(createStatusCellFactory());
        colAction.setCellFactory(createActionCellFactory());
        playerTable.setItems(playerList);

        colRoomID.setCellValueFactory(cell -> cell.getValue().idProperty());
        colRoomName.setCellValueFactory(cell -> cell.getValue().nameProperty());
        colRoomCode.setCellValueFactory(cell -> cell.getValue().codeProperty());
        colRoomStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
        colOwner.setCellValueFactory(cell -> cell.getValue().ownerProperty());
        colMembers.setCellValueFactory(cell -> cell.getValue().memberCountProperty());
        colRoomStatus.setCellFactory(createRoomStatusCellFactory());
        colRoomAction.setCellFactory(createRoomActionCellFactory());
        roomTable.setItems(roomList);

        logListView.setItems(ServerLogger.getLogs());
        logListView.setCellFactory(param -> new LogListCell());

        btnManagePlayers.setOnAction(e -> switchView(0));
        btnManageRooms.setOnAction(e -> switchView(1));
        btnSystemLogs.setOnAction(e -> switchView(2));

        btnClearLogs.setOnAction(e -> ServerLogger.getLogs().clear());
        btnReload.setOnAction(e -> reloadCurrentView());
    }

    private void switchView(int viewIndex) {
        currentView = viewIndex;

        playerTable.setVisible(false); playerTable.setManaged(false);
        roomTable.setVisible(false); roomTable.setManaged(false);
        logListView.setVisible(false); logListView.setManaged(false);
        btnReload.setVisible(true);
        btnClearLogs.setVisible(false); btnClearLogs.setManaged(false);

        if (viewIndex == 0) {
            lblHeader.setText("Quản lý người chơi");
            playerTable.setVisible(true); playerTable.setManaged(true);
            loadPlayerData();
        } else if (viewIndex == 1) {
            lblHeader.setText("Quản lý phòng chơi");
            roomTable.setVisible(true); roomTable.setManaged(true);
            loadRoomData();
        } else if (viewIndex == 2) {
            lblHeader.setText("Nhật ký hệ thống (Realtime)");
            logListView.setVisible(true); logListView.setManaged(true);
            btnReload.setVisible(false);
            btnClearLogs.setVisible(true); btnClearLogs.setManaged(true);
        }
    }

    private void reloadCurrentView() {
        if (currentView == 0) loadPlayerData();
        else loadRoomData();
    }

    private void loadPlayerData() {
        if (serverInstance == null) return;
        playerList.clear();

        try {
            List<PlayerState> dbPlayers = db.getAllPlayers();

            for (PlayerState p : dbPlayers) {
                boolean isOnline = serverInstance.isPlayerOnline(p.getPlayerID());
                String status = isOnline ? "Online 🟢" : "Offline 🔴";

                String roomLocation = "-";
                if (isOnline) {
                    int roomID = serverInstance.getPlayerCurrentRoom(p.getPlayerID());
                    roomLocation = (roomID != -1) ? "Phòng " + roomID : "Đang ở Sảnh";
                }

                playerList.add(new AdminPlayerRow(
                        p.getPlayerID(),
                        p.getUsername(),
                        p.getScore(),
                        status,
                        roomLocation
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể tải dữ liệu người chơi.");
        }
    }

    private void loadRoomData() {
        if (serverInstance == null) return;
        roomList.clear();
        try {
            List<AdminRoomRow> dbRooms = db.getAllRoomsForAdmin();
            for (AdminRoomRow r : dbRooms) {
                boolean isActive = serverInstance.isRoomActive(r.getId());
                r.setStatus(isActive ? "Open 🟢" : "Closed 🔴");
                roomList.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể tải dữ liệu phòng.");
        }
    }

    private Callback<TableColumn<AdminPlayerRow, Void>, TableCell<AdminPlayerRow, Void>> createActionCellFactory() {
        return param -> new TableCell<>() {
            private final Button btnDelete = new Button("Xóa");

            {
                btnDelete.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                btnDelete.setOnAction(event -> {
                    AdminPlayerRow row = getTableView().getItems().get(getIndex());
                    handleDeletePlayer(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnDelete);
                    // Disable nút nếu đang Online
                    AdminPlayerRow row = getTableView().getItems().get(getIndex());
                    btnDelete.setDisable(row.getStatus().contains("Online"));
                }
            }
        };
    }

    private Callback<TableColumn<AdminPlayerRow, String>, TableCell<AdminPlayerRow, String>> createStatusCellFactory() {
        return param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox hbox = new HBox(5);
                    hbox.setAlignment(Pos.CENTER_LEFT);

                    Circle dot = new Circle(5);
                    Label lblStatus = new Label();

                    if (item.contains("Online")) {
                        dot.setStyle("-fx-fill: #2ecc71;"); // Xanh lá
                        lblStatus.setText("Online");
                        lblStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                    } else {
                        dot.setStyle("-fx-fill: #e74c3c;"); // Đỏ
                        lblStatus.setText("Offline");
                        lblStatus.setStyle("-fx-text-fill: #7f8c8d;");
                    }

                    hbox.getChildren().addAll(dot, lblStatus);
                    setGraphic(hbox);
                    setText(null);
                }
            }
        };
    }

    private void handleDeletePlayer(AdminPlayerRow row) {
        if (row.getStatus().contains("Online")) {
            showAlert("Cảnh báo", "Không thể xóa tài khoản đang Online!");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận xóa");
        alert.setHeaderText("Xóa tài khoản: " + row.usernameProperty().get());
        alert.setContentText("CẢNH BÁO: Hành động này sẽ xóa toàn bộ phòng chơi và dữ liệu liên quan của người này. Không thể hoàn tác.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = serverInstance.performAdminDelete(row.getId());

            if (success) {
                showAlert("Thành công", "Đã xóa tài khoản và cập nhật hệ thống.");
                loadPlayerData();
            } else {
                showAlert("Thất bại", "Lỗi khi xóa tài khoản.");
            }
        }
    }

    private Callback<TableColumn<AdminRoomRow, String>, TableCell<AdminRoomRow, String>> createRoomStatusCellFactory() {
        return param -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null); setText(null);
                } else {
                    HBox hbox = new HBox(5);
                    hbox.setAlignment(Pos.CENTER_LEFT);
                    Circle dot = new Circle(5);
                    Label lbl = new Label();
                    if (item.contains("Open")) {
                        dot.setStyle("-fx-fill: #2ecc71;");
                        lbl.setText("Đang mở");
                        lbl.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                    } else {
                        dot.setStyle("-fx-fill: #e74c3c;");
                        lbl.setText("Đóng");
                        lbl.setStyle("-fx-text-fill: #7f8c8d;");
                    }
                    hbox.getChildren().addAll(dot, lbl);
                    setGraphic(hbox);
                }
            }
        };
    }

    private Callback<TableColumn<AdminRoomRow, Void>, TableCell<AdminRoomRow, Void>> createRoomActionCellFactory() {
        return param -> new TableCell<>() {
            private final Button btnDisband = new Button("Giải tán");
            private final Button btnManage = new Button("Quản lý");
            private final Button btnInteract = new Button("Tương tác");
            private final HBox pane = new HBox(5, btnManage, btnInteract, btnDisband);
            {
                btnDisband.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px;");
                btnDisband.setOnAction(event -> {
                    AdminRoomRow row = getTableView().getItems().get(getIndex());
                    handleDisbandRoom(row);
                });

                btnManage.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 10px;");
                btnManage.setOnAction(event -> {
                    AdminRoomRow row = getTableView().getItems().get(getIndex());
                    openRoomDetails(row);
                });

                btnInteract.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 10px;");
                btnInteract.setOnAction(event -> {
                    AdminRoomRow row = getTableView().getItems().get(getIndex());
                    handleInteractRoom(row);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null);
                else {
                    AdminRoomRow row = getTableView().getItems().get(getIndex());
                    // Chỉ hiện nút Tương tác nếu phòng đang mở (Open)
                    // (Vì phòng đóng thì không có ai để nhận tin nhắn)
                    if (row.statusProperty().get().contains("Open")) {
                        btnInteract.setDisable(false);
                    } else {
                        btnInteract.setDisable(true);
                    }
                    setGraphic(pane);
                }
            }
        };
    }

    private void handleDisbandRoom(AdminRoomRow row) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận giải tán");
        alert.setHeaderText("Giải tán phòng: " + row.getName());
        alert.setContentText("Hành động này sẽ đóng phòng (nếu mở) và xóa vĩnh viễn khỏi hệ thống.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = serverInstance.performAdminDeleteRoom(row.getId());
            if (success) {
                showAlert("Thành công", "Đã giải tán phòng.");
                loadRoomData();
            } else {
                showAlert("Thất bại", "Lỗi khi giải tán phòng.");
            }
        }
    }

    private void openRoomDetails(AdminRoomRow room) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/battleguess/battleguess/view/admin-room-details.fxml"));
            Parent root = loader.load();

            AdminRoomDetailsController controller = loader.getController();
            controller.initData(serverInstance, room);

            Stage stage = new Stage();
            stage.setTitle("Quản lý phòng: " + room.getName());
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Lỗi", "Không thể mở chi tiết phòng.");
        }
    }

    private void handleInteractRoom(AdminRoomRow row) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Tương tác Admin");
        dialog.setHeaderText("Gửi thông báo đến phòng: " + row.getName());
        dialog.setContentText("Nhập lời nhắn:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(message -> {
            if (message.trim().isEmpty()) return;

            boolean success = serverInstance.sendSystemMessage(row.getId(), message);

            if (success) {
                showAlert("Thành công", "Thông điệp đã được gửi!");
            } else {
                showAlert("Lỗi", "Phòng này hiện không hoạt động.");
                loadRoomData();
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class LogListCell extends ListCell<LogEntry> {
        @Override
        protected void updateItem(LogEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                // 1. Thời gian (Màu xám)
                Text timeText = new Text("[" + item.getFormattedTime() + "] ");
                timeText.setStyle("-fx-fill: #bdc3c7; -fx-font-family: 'Consolas'; -fx-font-size: 12px;");

                // 2. Cấp độ (Màu theo loại)
                Text levelText = new Text(String.format("%-7s", item.getLevel().toString()) + " : "); // Căn lề
                levelText.setStyle("-fx-font-family: 'Consolas'; -fx-font-weight: bold; -fx-font-size: 12px;");

                switch (item.getLevel()) {
                    case INFO:    levelText.setStyle(levelText.getStyle() + "-fx-fill: #3498db;"); break; // Blue
                    case WARN:    levelText.setStyle(levelText.getStyle() + "-fx-fill: #f39c12;"); break; // Orange
                    case ERROR:   levelText.setStyle(levelText.getStyle() + "-fx-fill: #e74c3c;"); break; // Red
                    case SUCCESS: levelText.setStyle(levelText.getStyle() + "-fx-fill: #2ecc71;"); break; // Green
                }

                // 3. Nội dung (Màu trắng)
                Text msgText = new Text(item.getMessage());
                msgText.setStyle("-fx-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");

                TextFlow flow = new TextFlow(timeText, levelText, msgText);
                setGraphic(flow);
                setStyle("-fx-background-color: transparent; -fx-padding: 2px;");
            }
        }
    }
}