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
