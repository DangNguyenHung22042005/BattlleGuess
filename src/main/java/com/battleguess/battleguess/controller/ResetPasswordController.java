package com.battleguess.battleguess.controller;

import com.battleguess.battleguess.Client;
import com.battleguess.battleguess.enum_to_manage_string.MessageType;
import com.battleguess.battleguess.network.Packet;
import com.battleguess.battleguess.network.request.ResetPasswordRequestPayload;
import com.battleguess.battleguess.network.response.GenericResponsePayload;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.io.IOException;

public class ResetPasswordController {
    @FXML private TextField txtPort;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label lblStatus;

    private String serverIp = "localhost";
    private Client client;

    @FXML
    private void onResetClicked() {
        String portText = txtPort.getText().trim();
        String username = txtUsername.getText().trim();
        String newPass = txtNewPassword.getText().trim();
        String confirmPass = txtConfirmPassword.getText().trim();

        if (portText.isEmpty() || username.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            setStatus("Vui lòng nhập đầy đủ thông tin!", true);
            return;
        }

        if (!isInputValid(username, newPass, confirmPass)) return;

        if (!newPass.equals(confirmPass)) {
            setStatus("Mật khẩu xác nhận không khớp!", true);
            return;
        }

        if (!connectToServer(portText)) return;

        ResetPasswordRequestPayload payload = new ResetPasswordRequestPayload(username, newPass);
        client.sendMessage(new Packet(MessageType.RESET_PASSWORD_REQUEST, payload));
    }

    @FXML
    private void onBackClicked() {
        if (client != null) client.disconnect();

        // Quay về màn hình Login
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/battleguess/battleguess/view/login-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(root, 400, 400));
            stage.setTitle("🎨 BattleGuess Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    private boolean connectToServer(String portText) {
        try {
            int port = Integer.parseInt(portText);

            if (client == null) {
                client = new Client(serverIp, port, this::handleServerResponse);
            }
            return true;
        } catch (NumberFormatException e) {
            setStatus("Port phải là số.", true);
            return false;
        } catch (IOException e) {
            setStatus("Không thể kết nối đến Server. Kiểm tra Port!", true);
            client = null;
            return false;
        }
    }

    private void handleServerResponse(Packet packet) {
        switch (packet.getType()) {
            case RESET_PASSWORD_SUCCESS:
                GenericResponsePayload successMsg = (GenericResponsePayload) packet.getData();
                setStatus(successMsg.getMessage(), false);
                break;

            case RESET_PASSWORD_FAILED:
            case ERROR:
                GenericResponsePayload errorMsg = (GenericResponsePayload) packet.getData();
                setStatus(errorMsg.getMessage(), true);
                if (packet.getType() == MessageType.ERROR) client = null;
                break;
        }
    }

    private void setStatus(String message, boolean isError) {
        Platform.runLater(() -> {
            lblStatus.setText(message);
            lblStatus.setTextFill(isError ? Color.web("#ffdddd") : Color.web("#aaffaa"));
        });
    }

    private boolean isInputValid(String username, String newPassword, String confirmPassword) {
        if (username.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            setStatus("Tên, mật khẩu và mật khẩu xác nhận không được để trống!", true);
            return false;
        }

        if (username.length() < 3) {
            setStatus("Tên đăng nhập phải có ít nhất 3 ký tự.", true);
            return false;
        }

        if (newPassword.length() < 6) {
            setStatus("Mật khẩu phải có ít nhất 6 ký tự.", true);
            return false;
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            setStatus("Tên chỉ chứa chữ cái, số, và dấu gạch dưới (_).", true);
            return false;
        }

        return true;
    }

    public void gracefulShutdown() {
        if (client != null) {
            client.disconnect();
            client = null; // Xóa tham chiếu
            System.out.println("ResetPassword Client disconnected via Close Request.");
        }
    }
}