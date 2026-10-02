package org.example.backend.labresource.bootstrap;

import org.example.backend.labresource.adapter.in.web.LabQueryController;
import org.example.backend.labresource.adapter.out.persistence.LabRecordReadMapper;
import org.example.backend.labresource.adapter.out.persistence.MyBatisLabRecordReadAdapter;
import org.example.backend.labresource.api.LabCatalogQuery;
import org.example.backend.labresource.application.DefaultLabAvailabilityQuery;
import org.example.backend.labresource.application.DefaultLabCatalogQuery;
import org.example.backend.labresource.application.port.out.DeviceCountPort;
import org.example.backend.labresource.application.port.out.ManagerNamePort;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Import;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class LabResourceRuntimeConfigurationTest {
    @Test
    void composesModuleMetadataWithRuntimeAdapters() {
        Import imported = LabResourceRuntimeConfiguration.class.getAnnotation(Import.class);
        MapperScan mapperScan = LabResourceRuntimeConfiguration.class.getAnnotation(MapperScan.class);

        assertNotNull(imported);
        assertTrue(Arrays.asList(imported.value()).contains(LabResourceModuleConfiguration.class));
        assertNotNull(mapperScan);
        assertEquals(LabRecordReadMapper.class, mapperScan.basePackageClasses()[0]);
    }

    @Test
    void wiresOneRecordPortIntoCatalogAvailabilityAndHttpAdapters() {
        LabResourceRuntimeConfiguration configuration = new LabResourceRuntimeConfiguration();
        var readPort = configuration.labRecordReadPort(mock(LabRecordReadMapper.class));
        ManagerNamePort managerNames = mock(ManagerNamePort.class);
        DeviceCountPort deviceCounts = mock(DeviceCountPort.class);
        LabCatalogQuery catalog = configuration.labCatalogQuery(readPort, managerNames, deviceCounts);

        assertInstanceOf(MyBatisLabRecordReadAdapter.class, readPort);
        assertInstanceOf(DefaultLabCatalogQuery.class, catalog);
        assertInstanceOf(DefaultLabAvailabilityQuery.class, configuration.labAvailabilityQuery(readPort));
        assertInstanceOf(LabQueryController.class, configuration.labQueryController(catalog));
    }
}
