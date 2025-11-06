package com.battleguess.battleguess.controller;

import com.battleguess.battleguess.database.DatabaseManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblStatus;

    private DatabaseManager db;

    public LoginController() {
        try {
            db = new DatabaseManager();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onLoginClicked(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            lblStatus.setText("⚠️ Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        try {
            if (!db.playerExists(username)) {
                lblStatus.setText("❌ Tên người chơi không tồn tại!");
                return;
            }

            int playerId = db.getPlayerId(username);
            // kiểm tra password
            String sql = "SELECT password FROM Player WHERE id = ?";
            var ps = db.getConnection().prepareStatement(sql);
            ps.setInt(1, playerId);
            var rs = ps.executeQuery();

            if (rs.next() && rs.getString("password").equals(password)) {
                lblStatus.setText("✅ Đăng nhập thành công!");
                openClientView(username);
            } else {
                lblStatus.setText("❌ Mật khẩu không đúng!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Lỗi khi đăng nhập!");
        }
    }

    @FXML
    private void onRegisterClicked(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            lblStatus.setText("⚠️ Nhập đầy đủ tên và mật khẩu để đăng ký!");
            return;
        }

        try {
            if (db.playerExists(username)) {
                lblStatus.setText("⚠️ Tên người chơi đã tồn tại!");
                return;
            }
            db.addPlayer(username, password);
            lblStatus.setText("✅ Đăng ký thành công! Bạn có thể đăng nhập.");
        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("❌ Lỗi khi đăng ký!");
        }
    }

    private void openClientView(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/battleguess/battleguess/view/client-view.fxml"));
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("🎮 BattleGuess - " + username);
        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Lỗi khi mở giao diện Client!");
        }
    }
}
