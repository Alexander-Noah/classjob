package org.example.backend.identityaccess.application.port.out;

public record UserCredentialRecord(long id, String username, String encodedPassword, String realName,
                                   String roleCode, String roleName, int status, String department,
                                   String phone, String email, String avatarUrl, String permissions) {
}
