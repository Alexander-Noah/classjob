package org.example.backend.identityaccess.internal;

import org.example.backend.identityaccess.api.AuthenticatedSession;
import org.example.backend.identityaccess.api.LoginCommand;
import org.example.backend.identityaccess.api.UserView;
import org.example.backend.identityaccess.internal.LoginAttemptPort;
import org.example.backend.identityaccess.internal.PasswordPort;
import org.example.backend.identityaccess.internal.TokenPort;
import org.example.backend.identityaccess.internal.UserCredentialPort;
import org.example.backend.identityaccess.internal.UserCredentialRecord;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultAuthenticationServiceTest {
    @Test
    void authenticatesActiveUserAndReturnsCompatibleSession() {
        var fixture = fixture(activeUser("teacher"), true, false);

        AuthenticatedSession session = fixture.service.authenticate(new LoginCommand("alice", "Secret123", "127.0.0.1"));

        assertEquals("signed-token", session.token());
        assertEquals("teacher", session.role());
        assertEquals("alice", session.user().username());
        assertEquals("success:alice|127.0.0.1", fixture.attemptEvent.get());
    }

    @Test
    void rejectsUnknownOrWrongCredentialsWithoutRevealingWhichFailed() {
        var fixture = fixture(null, false, false);

        InvalidCredentialsException error = assertThrows(InvalidCredentialsException.class,
                () -> fixture.service.authenticate(new LoginCommand("alice", "wrong", "127.0.0.1")));

        assertEquals("用户名或密码错误", error.getMessage());
        assertEquals("failure:alice|127.0.0.1", fixture.attemptEvent.get());
    }

    @Test
    void rejectsInactiveUserAndRetiredMaintenanceRole() {
        assertThrows(InvalidCredentialsException.class, () -> fixture(inactiveUser(), true, false).service
                .authenticate(new LoginCommand("alice", "Secret123", "ip")));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> fixture(activeUser("maintenance"), true, false).service
                .authenticate(new LoginCommand("alice", "Secret123", "ip")));
        assertEquals("角色已停用，请联系系统管理员重新分配角色", error.getMessage());
    }

    @Test
    void upgradesLegacyPasswordHashAfterSuccessfulAuthentication() {
        var fixture = fixture(activeUser("teacher"), true, true);

        fixture.service.authenticate(new LoginCommand("alice", "Secret123", "ip"));

        assertEquals("upgraded-hash", fixture.upgradedPassword.get());
    }

    private Fixture fixture(UserCredentialRecord user, boolean passwordMatches, boolean needsRehash) {
        AtomicReference<String> attemptEvent = new AtomicReference<>();
        AtomicReference<String> upgradedPassword = new AtomicReference<>();
        UserCredentialPort users = new UserCredentialPort() {
            @Override
            public Optional<UserCredentialRecord> findByUsername(String username) {
                return Optional.ofNullable(user);
            }

            @Override
            public void updatePassword(long userId, String encodedPassword) {
                upgradedPassword.set(encodedPassword);
            }
        };
        PasswordPort passwords = new PasswordPort() {
            public boolean matches(String raw, String encoded) { return passwordMatches; }
            public boolean needsRehash(String encoded) { return needsRehash; }
            public String encode(String raw) { return "upgraded-hash"; }
        };
        TokenPort tokens = new TokenPort() {
            public String issue(UserView user) { return "signed-token"; }
            public UserView validate(String token) { throw new UnsupportedOperationException(); }
        };
        LoginAttemptPort attempts = new LoginAttemptPort() {
            public void assertAllowed(String username, String clientAddress) { }
            public void recordFailure(String username, String clientAddress) { attemptEvent.set("failure:" + username + "|" + clientAddress); }
            public void recordSuccess(String username, String clientAddress) { attemptEvent.set("success:" + username + "|" + clientAddress); }
        };
        return new Fixture(new DefaultAuthenticationService(users, passwords, tokens, attempts), attemptEvent, upgradedPassword);
    }

    private UserCredentialRecord activeUser(String role) {
        return new UserCredentialRecord(7L, "alice", "stored-hash", "Alice", role, role, 1,
                "IT", "13800000000", "alice@example.com", "/avatar.png", "[]");
    }

    private UserCredentialRecord inactiveUser() {
        UserCredentialRecord active = activeUser("teacher");
        return new UserCredentialRecord(active.id(), active.username(), active.encodedPassword(), active.realName(),
                active.roleCode(), active.roleName(), 0, active.department(), active.phone(), active.email(), active.avatarUrl(), active.permissions());
    }

    private record Fixture(DefaultAuthenticationService service, AtomicReference<String> attemptEvent,
                           AtomicReference<String> upgradedPassword) { }
}
