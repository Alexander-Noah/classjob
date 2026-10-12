package org.example.backend.identityaccess.infrastructure;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryLoginAttemptAdapterTest {
    @Test
    void locksNormalizedUsernameAndClientPairAfterConfiguredFailures() {
        var limiter = new InMemoryLoginAttemptAdapter(3, 60,
                Clock.fixed(Instant.parse("2026-10-11T00:00:00Z"), ZoneOffset.UTC));
        limiter.recordFailure(" Alice ", "127.0.0.1");
        limiter.recordFailure("alice", "127.0.0.1");
        assertDoesNotThrow(() -> limiter.assertAllowed("ALICE", "127.0.0.1"));
        limiter.recordFailure("ALICE", "127.0.0.1");
        assertThrows(IllegalArgumentException.class, () -> limiter.assertAllowed("alice", "127.0.0.1"));
        assertDoesNotThrow(() -> limiter.assertAllowed("alice", "127.0.0.2"));
        limiter.recordSuccess("alice", "127.0.0.1");
        assertDoesNotThrow(() -> limiter.assertAllowed("alice", "127.0.0.1"));
    }
}
