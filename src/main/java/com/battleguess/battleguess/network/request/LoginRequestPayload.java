package com.battleguess.battleguess.network.request;

import com.battleguess.battleguess.network.Payload;

public class LoginRequestPayload implements Payload {
    private String username;
    private String password;

    public LoginRequestPayload(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}