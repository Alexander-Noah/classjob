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
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

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

    @Test
    void suppliesEmptyMetadataFallbacksWhenOwningModulesAreNotInstalled() {
        LabResourceRuntimeConfiguration configuration = new LabResourceRuntimeConfiguration();

        assertTrue(configuration.missingManagerNamePort().findNames(java.util.Set.of(1L)).isEmpty());
        assertTrue(configuration.missingDeviceCountPort().countByLabIds(java.util.Set.of(1L)).isEmpty());
    }

    @Test
    void catalogCanBeProxiedForOneReadOnlyTransaction() {
        try (var context = new AnnotationConfigApplicationContext(TransactionProxyConfiguration.class)) {
            assertNotNull(context.getBean(LabCatalogQuery.class));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class TransactionProxyConfiguration {
        @Bean
        PlatformTransactionManager transactionManager() {
            return mock(PlatformTransactionManager.class);
        }

        @Bean
        DefaultLabCatalogQuery labCatalogQuery() {
            return new DefaultLabCatalogQuery(
                    new MyBatisLabRecordReadAdapter(mock(LabRecordReadMapper.class)),
                    managerUserIds -> java.util.Map.of(),
                    labIds -> java.util.Map.of()
            );
        }
    }
}
