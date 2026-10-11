package org.example.backend.identityaccess.application.port.out;

public interface LoginAttemptPort {
    void assertAllowed(String username, String clientAddress);
    void recordFailure(String username, String clientAddress);
    void recordSuccess(String username, String clientAddress);
}
