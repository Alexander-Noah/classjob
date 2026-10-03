package org.example.backend.labresource.bootstrap;

import org.example.backend.labresource.adapter.in.web.LabQueryController;
import org.example.backend.labresource.adapter.out.persistence.LabRecordReadMapper;
import org.example.backend.labresource.adapter.out.persistence.MyBatisLabRecordReadAdapter;
import org.example.backend.labresource.api.LabAvailabilityQuery;
import org.example.backend.labresource.api.LabCatalogQuery;
import org.example.backend.labresource.application.DefaultLabAvailabilityQuery;
import org.example.backend.labresource.application.DefaultLabCatalogQuery;
import org.example.backend.labresource.application.port.out.DeviceCountPort;
import org.example.backend.labresource.application.port.out.LabRecordReadPort;
import org.example.backend.labresource.application.port.out.ManagerNamePort;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Configuration(proxyBeanMethods = false)
@MapperScan(basePackageClasses = LabRecordReadMapper.class)
@Import(LabResourceModuleConfiguration.class)
public class LabResourceRuntimeConfiguration {
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
