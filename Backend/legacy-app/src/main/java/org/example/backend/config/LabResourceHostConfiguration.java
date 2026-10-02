package org.example.backend.config;

import org.example.backend.adapter.labmetadata.LegacyLabMetadataAdapter;
import org.example.backend.mapper.LabResourceMetadataMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class LabResourceHostConfiguration {
    @Bean
    public LegacyLabMetadataAdapter legacyLabMetadataAdapter(LabResourceMetadataMapper mapper) {
        return new LegacyLabMetadataAdapter(mapper);
    }
}
