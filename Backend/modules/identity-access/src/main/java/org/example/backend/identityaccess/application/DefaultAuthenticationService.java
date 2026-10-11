package org.example.backend.identityaccess.application;

import org.example.backend.identityaccess.api.AuthenticatedSession;
import org.example.backend.identityaccess.api.AuthenticationUseCase;
import org.example.backend.identityaccess.api.LoginCommand;
import org.example.backend.identityaccess.api.UserView;
import org.example.backend.identityaccess.application.port.out.LoginAttemptPort;
import org.example.backend.identityaccess.application.port.out.PasswordPort;
import org.example.backend.identityaccess.application.port.out.TokenPort;
import org.example.backend.identityaccess.application.port.out.UserCredentialPort;
import org.example.backend.identityaccess.application.port.out.UserCredentialRecord;

public final class DefaultAuthenticationService implements AuthenticationUseCase {
    private static final String RETIRED_ROLE_MESSAGE = "角色已停用，请联系系统管理员重新分配角色";
    private final UserCredentialPort users;
    private final PasswordPort passwords;
    private final TokenPort tokens;
    private final LoginAttemptPort attempts;

    public DefaultAuthenticationService(UserCredentialPort users, PasswordPort passwords, TokenPort tokens,
                                        LoginAttemptPort attempts) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.attempts = attempts;
    }

    @Override
    public AuthenticatedSession authenticate(LoginCommand command) {
        attempts.assertAllowed(command.username(), command.clientAddress());
        UserCredentialRecord record = users.findByUsername(command.username()).orElse(null);
        if (record == null || record.status() != 1 || !passwords.matches(command.password(), record.encodedPassword())) {
            attempts.recordFailure(command.username(), command.clientAddress());
            throw new InvalidCredentialsException();
        }
        if ("maintenance".equals(record.roleCode()) || "维修人员".equals(record.roleName())) {
            throw new IllegalArgumentException(RETIRED_ROLE_MESSAGE);
        }
        if (passwords.needsRehash(record.encodedPassword())) {
            users.updatePassword(record.id(), passwords.encode(command.password()));
        }
        UserView user = new UserView(record.id(), record.username(), record.realName(), record.roleCode(),
                record.roleName(), record.department(), record.phone(), record.email(), record.avatarUrl(), record.permissions());
        attempts.recordSuccess(command.username(), command.clientAddress());
        return new AuthenticatedSession(tokens.issue(user), user.roleCode(), user);
    }
}
