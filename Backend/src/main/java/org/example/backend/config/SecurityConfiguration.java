/*
 * 安全策略配置组件，声明应用启动或框架集成时使用的行为。
 */
package org.example.backend.config;

import org.example.backend.security.AuthorizationPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
    @Bean
    public AuthorizationPolicy defaultDenyAuthorizationPolicy() {
        return (authority, action) -> false;
    }
}
