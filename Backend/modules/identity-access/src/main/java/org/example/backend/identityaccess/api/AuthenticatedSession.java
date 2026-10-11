package org.example.backend.identityaccess.api;

public record AuthenticatedSession(String token, String role, UserView user) {
}
