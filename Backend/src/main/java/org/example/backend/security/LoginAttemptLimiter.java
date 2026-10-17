/*
 * 登录的服务端安全组件；调用方应保持身份认证与权限判断边界。
 */
package org.example.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptLimiter {
    /** 单 JVM 的失败计数，进程重启即丢失；多实例部署时不是全局限流。 */
    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final long lockSeconds;

    /**
     * 构造 LoginAttemptLimiter，初始化当前实例。
     * @param maxAttempts max尝试参数。
     * @param lockSeconds lockSeconds参数。
     */
    public LoginAttemptLimiter(
            @Value("${smart-lab.security.login-rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${smart-lab.security.login-rate-limit.lock-seconds:900}") long lockSeconds
    ) {
        this.maxAttempts = Math.max(maxAttempts, 3);
        this.lockSeconds = Math.max(lockSeconds, 60);
    }

    /**
     * 在密码校验前检查当前账号/IP 组合的锁定状态；不会增加失败次数。
     * @throws IllegalArgumentException 尚在锁定期内，消息给出剩余秒数
     */
    public void assertAllowed(String username, HttpServletRequest request) {
        AttemptState state = attempts.get(key(username, request));
        if (state == null || state.lockedUntilEpochSecond <= Instant.now().getEpochSecond()) {
            return;
        }
        long waitSeconds = Math.max(1, state.lockedUntilEpochSecond - Instant.now().getEpochSecond());
        throw new IllegalArgumentException("登录失败次数过多，请 " + waitSeconds + " 秒后再试");
    }

    /** 登录成功后只清除相同账号/IP 的计数，不影响其他来源。 */
    public void recordSuccess(String username, HttpServletRequest request) {
        attempts.remove(key(username, request));
    }

    /**
     * 记录失败。
     * @param username username参数。
     * @param request 请求参数。
     */
    public void recordFailure(String username, HttpServletRequest request) {
        String key = key(username, request);
        // compute 串行更新同一键，避免并行失败请求丢失递增；检查和计数仍是两个调用。
        attempts.compute(key, /** 处理 compute 调用中的回调逻辑。 入参：ignored、state。 */ (ignored, state) -> {
            long now = Instant.now().getEpochSecond();
            AttemptState next = state == null
                    || (state.lockedUntilEpochSecond > 0 && state.lockedUntilEpochSecond <= now)
                    ? new AttemptState() : state;
            next.failureCount++;
            if (next.failureCount >= maxAttempts) {
                next.lockedUntilEpochSecond = now + lockSeconds;
            }
            return next;
        });
    }

    /**
     * 键的处理逻辑。
     * @param username username参数。
     * @param request 请求参数。
     * @return String 类型的处理结果。
     */
    private String key(String username, HttpServletRequest request) {
        String normalizedUsername = username == null || username.isBlank()
                ? "unknown"
                : username.trim().toLowerCase(Locale.ROOT);
        return normalizedUsername + "|" + clientIp(request);
    }

    // 依次采用代理头和连接地址；部署端需由可信代理清理外部伪造的转发头。
    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static class AttemptState {
        private int failureCount;
        private long lockedUntilEpochSecond;
    }
}
