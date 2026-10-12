package org.example.backend.identityaccess.infrastructure;

import org.example.backend.identityaccess.api.UserView;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HmacJwtTokenAdapterTest {
    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-11T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void roundTripsRequiredIdentityClaims() {
        HmacJwtTokenAdapter tokens = new HmacJwtTokenAdapter(SECRET, 7200, "smart-lab", clock);
        UserView user = user();

        UserView claims = tokens.validate(tokens.issue(user));

        assertEquals(user.id(), claims.id());
        assertEquals(user.username(), claims.username());
        assertEquals(user.roleCode(), claims.roleCode());
    }

    @Test
    void rejectsWeakSecretTamperingWrongIssuerAndExpiredToken() {
        assertThrows(IllegalStateException.class, () -> new HmacJwtTokenAdapter("weak", 7200, "smart-lab", clock));
        HmacJwtTokenAdapter tokens = new HmacJwtTokenAdapter(SECRET, 300, "smart-lab", clock);
        String token = tokens.issue(user());
        assertThrows(IllegalArgumentException.class, () -> tokens.validate(token.substring(0, token.length() - 1) + "x"));
        assertThrows(IllegalArgumentException.class, () -> new HmacJwtTokenAdapter(SECRET, 300, "other", clock).validate(token));
        HmacJwtTokenAdapter future = new HmacJwtTokenAdapter(SECRET, 300, "smart-lab",
                Clock.fixed(Instant.parse("2026-10-11T00:06:00Z"), ZoneOffset.UTC));
        assertThrows(IllegalArgumentException.class, () -> future.validate(token));
    }

    private UserView user() {
        return new UserView(7L, "alice", "Alice", "teacher", "教师", "IT", null, null, null, "[]");
    }
}
