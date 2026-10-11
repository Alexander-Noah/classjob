package org.example.backend.identityaccess.api;

public record UserView(long id, String username, String realName, String roleCode, String roleName,
                       String department, String phone, String email, String avatarUrl, String permissions) {
}
