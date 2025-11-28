package com.battleguess.battleguess.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogEntry {
    public enum Level { INFO, WARN, ERROR, SUCCESS }

    private final LocalDateTime timestamp;
    private final Level level;
    private final String message;

    public LogEntry(Level level, String message) {
        this.timestamp = LocalDateTime.now();
        this.level = level;
        this.message = message;
    }

    public String getFormattedTime() {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")); // Giờ:Phút:Giây
    }

    public Level getLevel() { return level; }
    public String getMessage() { return message; }
}