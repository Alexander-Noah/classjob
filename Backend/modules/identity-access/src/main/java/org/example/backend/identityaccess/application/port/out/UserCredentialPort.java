package org.example.backend.identityaccess.application.port.out;

import java.util.Optional;

public interface UserCredentialPort {
    Optional<UserCredentialRecord> findByUsername(String username);
    void updatePassword(long userId, String encodedPassword);
}
