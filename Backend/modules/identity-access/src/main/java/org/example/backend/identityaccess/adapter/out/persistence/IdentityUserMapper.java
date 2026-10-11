package org.example.backend.identityaccess.adapter.out.persistence;

import org.apache.ibatis.annotations.Param;
import org.example.backend.identityaccess.application.port.out.UserCredentialPort;
import org.example.backend.identityaccess.application.port.out.UserCredentialRecord;

import java.util.Optional;

public interface IdentityUserMapper extends UserCredentialPort {
    @Override
    Optional<UserCredentialRecord> findByUsername(@Param("username") String username);

    @Override
    void updatePassword(@Param("userId") long userId, @Param("encodedPassword") String encodedPassword);
}
