package com.battleguess.battleguess.service;

import com.battleguess.battleguess.model.LogEntry;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ServerLogger {
    // Danh sách Log quan sát được (cho UI)
    private static final ObservableList<LogEntry> logs = FXCollections.observableArrayList();

    // Giới hạn số lượng log để tránh tràn RAM (ví dụ: giữ 1000 dòng cuối)
    private static final int MAX_LOGS = 1000;

    public static ObservableList<LogEntry> getLogs() {
        return logs;
    }

    private static void addLog(LogEntry.Level level, String message) {
        // In ra console của IDE để debug
        System.out.println("[" + level + "] " + message);

        // Cập nhật UI (phải chạy trên JavaFX Thread)
        Platform.runLater(() -> {
            logs.add(0, new LogEntry(level, message)); // Thêm vào đầu (mới nhất lên trên)
            if (logs.size() > MAX_LOGS) {
                logs.remove(logs.size() - 1); // Xóa cũ nhất
            }
        });
    }

    // Các hàm tiện ích
    public static void info(String message) { addLog(LogEntry.Level.INFO, message); }
    public static void warn(String message) { addLog(LogEntry.Level.WARN, message); }
    public static void error(String message) { addLog(LogEntry.Level.ERROR, message); }
    public static void success(String message) { addLog(LogEntry.Level.SUCCESS, message); }
}