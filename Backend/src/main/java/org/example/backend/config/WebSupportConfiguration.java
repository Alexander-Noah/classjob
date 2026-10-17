/*
 * WebSupportConfiguration配置组件，声明应用启动或框架集成时使用的行为。
 */
package org.example.backend.config;

import org.example.backend.service.CorrelationIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration(proxyBeanMethods = false)
public class WebSupportConfiguration {
    /**
     * correlation标识服务商的处理逻辑。
     * @return CorrelationIdProvider 类型的处理结果。
     */
    @Bean
    public CorrelationIdProvider correlationIdProvider() {
        return /** 处理当前调用传入的回调逻辑。 */ () -> UUID.randomUUID().toString();
    }
}
