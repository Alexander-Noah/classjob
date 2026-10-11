package org.example.backend.identityaccess.api;

public record LoginCommand(String username, String password, String clientAddress) {
    public LoginCommand {
        username = username == null ? "" : username.trim();
        password = password == null ? "" : password;
        clientAddress = clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress.trim();
    }
}
