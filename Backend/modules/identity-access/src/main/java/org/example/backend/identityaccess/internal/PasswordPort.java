package org.example.backend.identityaccess.internal;

public interface PasswordPort {
    boolean matches(String rawPassword, String encodedPassword);
    boolean needsRehash(String encodedPassword);
    String encode(String rawPassword);
}
