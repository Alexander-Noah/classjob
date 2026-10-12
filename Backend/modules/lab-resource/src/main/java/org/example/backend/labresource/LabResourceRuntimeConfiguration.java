package org.example.backend.labresource;

import org.example.backend.labresource.web.LabQueryController;
import org.example.backend.labresource.infrastructure.LabRecordReadMapper;
import org.example.backend.labresource.infrastructure.MyBatisLabRecordReadAdapter;
import org.example.backend.labresource.api.LabAvailabilityQuery;
import org.example.backend.labresource.api.LabCatalogQuery;
import org.example.backend.labresource.application.DefaultLabAvailabilityQuery;
import org.example.backend.labresource.application.DefaultLabCatalogQuery;
import org.example.backend.labresource.application.DeviceCountPort;
import org.example.backend.labresource.application.LabRecordReadPort;
import org.example.backend.labresource.application.ManagerNamePort;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Configuration(proxyBeanMethods = false)
@MapperScan(basePackageClasses = LabRecordReadMapper.class)
@Import(LabResourceModuleConfiguration.class)
public class LabResourceRuntimeConfiguration {
    @Bean
    @ConditionalOnMissingBean(ManagerNamePort.class)
    public ManagerNamePort missingManagerNamePort() {
        return managerUserIds -> java.util.Map.of();
    }

    @Bean
    @ConditionalOnMissingBean(DeviceCountPort.class)
    public DeviceCountPort missingDeviceCountPort() {
        return labIds -> java.util.Map.of();
    }

    @Bean
    public LabRecordReadPort labRecordReadPort(LabRecordReadMapper mapper) {
        return new MyBatisLabRecordReadAdapter(mapper);
    }

    @Bean
    public LabCatalogQuery labCatalogQuery(
            LabRecordReadPort records,
            ManagerNamePort managerNames,
            DeviceCountPort deviceCounts
    ) {
        return new DefaultLabCatalogQuery(records, managerNames, deviceCounts);
    }

    @Bean
    @Primary
    public LabAvailabilityQuery labAvailabilityQuery(LabRecordReadPort records) {
        return new DefaultLabAvailabilityQuery(records);
    }

    @Bean
    public LabQueryController labQueryController(LabCatalogQuery query) {
        return new LabQueryController(query);
    }
}
