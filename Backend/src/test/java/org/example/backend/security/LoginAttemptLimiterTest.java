/*
 * 登录回归测试；测试夹具与生产配置分离。
 * 覆盖场景：repeatedFailuresLockTheSameAccountAndClient、successfulLoginClearsAccumulatedFailures。
 */
package org.example.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptLimiterTest {
    @Test
    void repeatedFailuresLockTheSameAccountAndClient() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(3, 60);
        MockHttpServletRequest request = request("127.0.0.1");

        for (int attempt = 0; attempt < 3; attempt++) {
            assertDoesNotThrow(() -> limiter.assertAllowed("Alice", request));
            limiter.recordFailure("Alice", request);
        }

        assertThrows(IllegalArgumentException.class, () -> limiter.assertAllowed(" alice ", request));
        assertDoesNotThrow(() -> limiter.assertAllowed("Bob", request));
        assertDoesNotThrow(() -> limiter.assertAllowed("Alice", request("127.0.0.2")));
    }

    @Test
    void successfulLoginClearsAccumulatedFailures() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(3, 60);
        MockHttpServletRequest request = request("127.0.0.1");
        limiter.recordFailure("Alice", request);
        limiter.recordFailure("Alice", request);
        limiter.recordSuccess("Alice", request);
        limiter.recordFailure("Alice", request);
        limiter.recordFailure("Alice", request);

        assertDoesNotThrow(() -> limiter.assertAllowed("Alice", request));
        limiter.recordFailure("Alice", request);
        assertThrows(IllegalArgumentException.class, () -> limiter.assertAllowed("Alice", request));
    }

    private MockHttpServletRequest request(String address) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(address);
        return request;
    }
}
