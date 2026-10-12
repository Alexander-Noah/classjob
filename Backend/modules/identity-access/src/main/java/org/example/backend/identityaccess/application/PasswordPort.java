package org.example.backend.identityaccess.application;

public interface PasswordPort {
    boolean matches(String rawPassword, String encodedPassword);
    boolean needsRehash(String encodedPassword);
    String encode(String rawPassword);
}
