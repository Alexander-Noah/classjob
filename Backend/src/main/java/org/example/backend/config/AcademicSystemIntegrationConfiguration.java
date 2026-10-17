/*
 * 教务系统接入配置组件，声明应用启动或框架集成时使用的行为。
 */
package org.example.backend.config;

import org.example.backend.service.AcademicSystemAvailability;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AcademicSystemIntegrationConfiguration {
    /**
     * 教务SystemAvailability的处理逻辑。
     * @return AcademicSystemAvailability 类型的处理结果。
     */
    @Bean
    public AcademicSystemAvailability academicSystemAvailability() {
        return new AcademicSystemAvailability(false);
    }
}
