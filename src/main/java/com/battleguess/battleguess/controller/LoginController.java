package com.battleguess.battleguess.controller;

import com.battleguess.battleguess.Client;
import com.battleguess.battleguess.enum_to_manage_string.MessageType;
import com.battleguess.battleguess.network.response.GenericResponsePayload;
import com.battleguess.battleguess.network.request.LoginRequestPayload;
import com.battleguess.battleguess.network.response.LoginSuccessPayload;
import com.battleguess.battleguess.network.Packet;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblStatus;
    @FXML private TextField txtPort;

    private String ipAddress = "localhost"; //localhost //192.168.1.21
    private Client client;

    @FXML
    private void onLoginClicked(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (!isInputValid(username, password)) return;
        if (!validateConnection(password)) return;

        LoginRequestPayload payload = new LoginRequestPayload(username, password);
        client.sendMessage(new Packet(MessageType.LOGIN_REQUEST, payload));
    }

    @FXML
    private void onRegisterClicked(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (!isInputValid(username, password)) return;
        if (!validateConnection(password)) return;

        LoginRequestPayload payload = new LoginRequestPayload(username, password);
        client.sendMessage(new Packet(MessageType.REGISTER_REQUEST, payload));
    }

    private boolean isInputValid(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            setStatus("Tên và mật khẩu không được để trống!", true);
            return false;
        }

        if (username.length() < 3) {
            setStatus("Tên đăng nhập phải có ít nhất 3 ký tự.", true);
            return false;
        }

        if (password.length() < 6) {
            setStatus("Mật khẩu phải có ít nhất 6 ký tự.", true);
            return false;
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            setStatus("Tên chỉ chứa chữ cái, số, và dấu gạch dưới (_).", true);
            return false;
        }

        return true;
    }

    private void setStatus(String message, boolean isError) {
        lblStatus.setText(message);
        if (isError) {
            lblStatus.setTextFill(Color.RED);
        } else {
            lblStatus.setTextFill(Color.GREEN);
        }
    }

    private boolean validateConnection(String password) {
        String portText = txtPort.getText().trim();
        String username = txtUsername.getText().trim();

        if (portText.isEmpty() || username.isEmpty() || password.isEmpty()) {
            setStatus("Vui lòng nhập đầy đủ Port, Tên và Mật khẩu!", true);
            return false;
        }

        try {
            int port = Integer.parseInt(portText);
            if (client == null) {
                client = new Client(ipAddress, port, this::handleServerResponse);
            }
            return true;
        } catch (NumberFormatException e) {
            setStatus("Port phải là số.", true);
            return false;
        } catch (IOException e) {
            setStatus("Port " + portText + " không khả dụng hoặc Server chưa chạy.", true);
            client = null;
            return false;
        }
    }

    private void handleServerResponse(Packet packet) {
        switch (packet.getType()) {
            case LOGIN_SUCCESS:
                LoginSuccessPayload successData = (LoginSuccessPayload) packet.getData();
                setStatus("Đăng nhập thành công!", false);
                openClientView(successData.getPlayerID(), successData.getUsername(), client);
                break;
            case LOGIN_FAILED:
            case REGISTER_SUCCESS:
            case REGISTER_FAILED:
            case ERROR:
                GenericResponsePayload response = (GenericResponsePayload) packet.getData();
                setStatus(response.getMessage(), (packet.getType() != MessageType.REGISTER_SUCCESS));
                if(packet.getType() == MessageType.ERROR) client = null;
                break;
        }
    }

    private void openClientView(int playerID, String username, Client connectedClient) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/battleguess/battleguess/view/client-view.fxml"));
            Parent root = loader.load();

            ClientController clientController = loader.getController();
            clientController.initData(playerID, username, connectedClient);

            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 700));
            stage.setTitle("🎮 BattleGuess - " + username);
            stage.centerOnScreen();

            // --- LOGIC MỚI: BẮT SỰ KIỆN NHẤN NÚT "X" ---
            stage.setOnCloseRequest(event -> {
                // Hỏi ClientController xem có đang trong phòng không
                if (clientController.isUserInRoom()) {
                    // 1. NẾU ĐANG TRONG PHÒNG: Cảnh báo và HỦY BỎ việc đóng
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Không thể đóng ứng dụng");
                    alert.setHeaderText("Bạn đang ở trong một phòng chơi!");
                    alert.setContentText("Bạn phải 'Thoát (Về sảnh)' hoặc 'Đóng phòng' trước khi đóng ứng dụng.");
                    alert.showAndWait();

                    // Hủy sự kiện đóng (quan trọng nhất)
                    event.consume();
                } else {
                    // 2. NẾU ĐANG Ở SẢNH: Cho phép đóng
                    // Ngắt kết nối client một cách an toàn
                    clientController.gracefulShutdown();
                    // (Không gọi event.consume(), để ứng dụng tự tắt)
                }
            });
            // --- KẾT THÚC LOGIC MỚI ---

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            setStatus("Lỗi khi mở giao diện chính!", true);
        }
    }
}