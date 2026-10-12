package org.example.backend.identityaccess.infrastructure;

import org.apache.ibatis.annotations.Param;
import org.example.backend.identityaccess.application.UserCredentialPort;
import org.example.backend.identityaccess.application.UserCredentialRecord;

import java.util.Optional;

public interface IdentityUserMapper extends UserCredentialPort {
    @Override
    Optional<UserCredentialRecord> findByUsername(@Param("username") String username);

    @Override
    void updatePassword(@Param("userId") long userId, @Param("encodedPassword") String encodedPassword);
}
