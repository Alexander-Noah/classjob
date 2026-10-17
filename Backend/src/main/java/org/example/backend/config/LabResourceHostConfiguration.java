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
    /**
     * 实验室Metadata服务的处理逻辑。
     * @param mapper 数据映射器参数。
     * @return LabMetadataService 类型的处理结果。
     */
    @Bean
    public LabMetadataService labMetadataService(LabResourceMetadataMapper mapper) {
        return new LabMetadataService(mapper);
    }
}
