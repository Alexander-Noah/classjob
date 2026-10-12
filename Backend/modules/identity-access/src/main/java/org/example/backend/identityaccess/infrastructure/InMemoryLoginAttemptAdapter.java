package org.example.backend.identityaccess.infrastructure;

import org.example.backend.identityaccess.application.LoginAttemptPort;

import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryLoginAttemptAdapter implements LoginAttemptPort {
    private final Map<String, State> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long lockSeconds;
    private final Clock clock;

    public InMemoryLoginAttemptAdapter(int maxAttempts, long lockSeconds) {
        this(maxAttempts, lockSeconds, Clock.systemUTC());
    }

    InMemoryLoginAttemptAdapter(int maxAttempts, long lockSeconds, Clock clock) {
        this.maxAttempts = Math.max(maxAttempts, 3);
        this.lockSeconds = Math.max(lockSeconds, 60);
        this.clock = clock;
    }

    @Override
    public void assertAllowed(String username, String clientAddress) {
        State state = attempts.get(key(username, clientAddress));
        long now = clock.instant().getEpochSecond();
        if (state != null && state.lockedUntil > now) {
            throw new IllegalArgumentException("登录失败次数过多，请 " + (state.lockedUntil - now) + " 秒后再试");
        }
    }

    @Override
    public void recordFailure(String username, String clientAddress) {
        attempts.compute(key(username, clientAddress), (ignored, current) -> {
            long now = clock.instant().getEpochSecond();
            State state = current == null || (current.lockedUntil > 0 && current.lockedUntil <= now) ? new State() : current;
            state.failures++;
            if (state.failures >= maxAttempts) state.lockedUntil = now + lockSeconds;
            return state;
        });
    }

    @Override
    public void recordSuccess(String username, String clientAddress) {
        attempts.remove(key(username, clientAddress));
    }

    private String key(String username, String clientAddress) {
        String user = username == null || username.isBlank() ? "unknown" : username.trim().toLowerCase(Locale.ROOT);
        String client = clientAddress == null || clientAddress.isBlank() ? "unknown" : clientAddress.trim();
        return user + "|" + client;
    }

    private static final class State { int failures; long lockedUntil; }
}
