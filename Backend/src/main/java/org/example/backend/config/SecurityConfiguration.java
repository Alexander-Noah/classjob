/*
 * 安全策略配置组件，声明应用启动或框架集成时使用的行为。
 */
package org.example.backend.config;

import org.example.backend.security.AuthorizationPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
    /**
     * 提供默认DenyAuthorizationPolicy。
     * @return AuthorizationPolicy 类型的处理结果。
     */
    @Bean
    public AuthorizationPolicy defaultDenyAuthorizationPolicy() {
        return /** 处理当前调用传入的回调逻辑。 入参：authority、action。 */ (authority, action) -> false;
    }
}
