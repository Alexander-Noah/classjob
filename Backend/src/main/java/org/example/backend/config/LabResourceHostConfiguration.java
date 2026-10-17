/*
 * 实验室资源配置组件，声明应用启动或框架集成时使用的行为。
 */
package org.example.backend.config;

import org.example.backend.mapper.LabResourceMetadataMapper;
import org.example.backend.service.LabMetadataService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LabResourceHostConfiguration {
    @Bean
    public LabMetadataService labMetadataService(LabResourceMetadataMapper mapper) {
        return new LabMetadataService(mapper);
    }
}
