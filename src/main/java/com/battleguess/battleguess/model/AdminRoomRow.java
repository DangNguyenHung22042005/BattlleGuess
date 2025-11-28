package com.battleguess.battleguess.model;

import javafx.beans.property.*;

public class AdminRoomRow {
    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty owner;
    private final StringProperty status;
    private final IntegerProperty memberCount;
    private final StringProperty code;

    public AdminRoomRow(int id, String name, String owner, String status, int memberCount, String code) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.owner = new SimpleStringProperty(owner);
        this.status = new SimpleStringProperty(status);
        this.memberCount = new SimpleIntegerProperty(memberCount);
        this.code = new SimpleStringProperty(code);
    }

    public IntegerProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public StringProperty ownerProperty() { return owner; }
    public StringProperty statusProperty() { return status; }
    public IntegerProperty memberCountProperty() { return memberCount; }
    public StringProperty codeProperty() { return code; }

    // Setters (để cập nhật trạng thái)
    public void setStatus(String newStatus) { this.status.set(newStatus); }
    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
}