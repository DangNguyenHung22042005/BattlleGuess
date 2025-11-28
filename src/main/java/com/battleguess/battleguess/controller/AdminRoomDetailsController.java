package com.battleguess.battleguess.controller;

import com.battleguess.battleguess.Server;
import com.battleguess.battleguess.database.DatabaseManager;
import com.battleguess.battleguess.model.AdminRoomMemberRow;
import com.battleguess.battleguess.model.AdminRoomRow;
import com.battleguess.battleguess.model.PlayerState;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AdminRoomDetailsController {
    @FXML private Label lblRoomName;
    @FXML private TableView<AdminRoomMemberRow> memberTable;
    @FXML private TableColumn<AdminRoomMemberRow, Number> colID;
    @FXML private TableColumn<AdminRoomMemberRow, String> colName;
    @FXML private TableColumn<AdminRoomMemberRow, String> colRole;
    @FXML private TableColumn<AdminRoomMemberRow, String> colStatus;
    @FXML private TableColumn<AdminRoomMemberRow, Void> colAction;

    private Server serverInstance;
    private DatabaseManager db;
    private AdminRoomRow currentRoom;
    private ObservableList<AdminRoomMemberRow> memberList = FXCollections.observableArrayList();

    public void initData(Server server, AdminRoomRow room) {
        this.serverInstance = server;
        this.db = server.getDatabaseManager();
        this.currentRoom = room;

        lblRoomName.setText("Quản lý phòng: " + room.getName() + " (ID: " + room.getId() + ")");
        loadMembers();
    }

    @FXML
    private void initialize() {
        colID.setCellValueFactory(cell -> cell.getValue().playerIDProperty());
        colName.setCellValueFactory(cell -> cell.getValue().usernameProperty());
        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());

        // Cột Vai trò (Custom)
        colRole.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        cell.getValue().isOwner() ? "👑 Chủ phòng" : "Thành viên"
                )
        );

        // Cột Hành động (Nút Kick)
        colAction.setCellFactory(createActionCellFactory());

        memberTable.setItems(memberList);
    }

    private void loadMembers() {
        if (serverInstance == null) return;
        memberList.clear();
        try {
            int ownerID = db.getRoomOwner(currentRoom.getId());

            // Hàm này trả về List<PlayerState>
            List<PlayerState> members = db.getRoomMembersBasicInfo(currentRoom.getId(), ownerID);

            for (PlayerState p : members) {
                // Check online status bằng Server Memory
                boolean isOnline = serverInstance.isPlayerOnline(p.getPlayerID());

                memberList.add(new AdminRoomMemberRow(
                        p.getPlayerID(),
                        p.getUsername(),
                        p.getScore(),
                        p.isOwner(),
                        isOnline
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Callback<TableColumn<AdminRoomMemberRow, Void>, TableCell<AdminRoomMemberRow, Void>> createActionCellFactory() {
        return param -> new TableCell<>() {
            private final Button btnKick = new Button("Kick");
            {
                btnKick.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                btnKick.setOnAction(event -> {
                    AdminRoomMemberRow row = getTableView().getItems().get(getIndex());
                    handleKickMember(row);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    AdminRoomMemberRow row = getTableView().getItems().get(getIndex());
                    // Không cho phép Kick chủ phòng
                    if (row.isOwner()) {
                        setGraphic(null);
                    } else {
                        setGraphic(btnKick);
                    }
                }
            }
        };
    }

    private void handleKickMember(AdminRoomMemberRow row) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận Kick");
        alert.setHeaderText("Kick người chơi: " + row.getUsername());
        alert.setContentText("Bạn có chắc chắn muốn đá người này ra khỏi phòng?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = serverInstance.performAdminKick(currentRoom.getId(), row.getPlayerID());

            if (success) {
                loadMembers();
            } else {
                Alert error = new Alert(Alert.AlertType.ERROR, "Lỗi khi kick thành viên.");
                error.show();
            }
        }
    }
}