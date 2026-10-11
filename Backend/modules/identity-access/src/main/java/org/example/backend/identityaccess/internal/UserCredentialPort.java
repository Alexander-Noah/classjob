package org.example.backend.identityaccess.internal;

import java.util.Optional;

public interface UserCredentialPort {
    Optional<UserCredentialRecord> findByUsername(String username);
    void updatePassword(long userId, String encodedPassword);
}
