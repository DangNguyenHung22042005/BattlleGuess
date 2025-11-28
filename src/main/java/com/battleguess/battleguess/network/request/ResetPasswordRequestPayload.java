package com.battleguess.battleguess.network.request;

import com.battleguess.battleguess.network.Payload;

public class ResetPasswordRequestPayload implements Payload {
    private String username;
    private String newPassword;

    public ResetPasswordRequestPayload(String username, String newPassword) {
        this.username = username;
        this.newPassword = newPassword;
    }

    public String getUsername() { return username; }
    public String getNewPassword() { return newPassword; }
}