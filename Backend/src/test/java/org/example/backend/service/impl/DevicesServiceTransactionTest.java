/*
 * 设备管理回归测试；测试夹具与生产配置分离。
 * 覆盖场景：transferReturnsRereadDeviceAndHistory、inventoryReturnsRereadDeviceAndHistory、springProxyExposesTransactionsForTransferAndInventory。
 */
package org.example.backend.service.impl;

import org.example.backend.entity.DeviceInventoryRecordEntity;
import org.example.backend.entity.DeviceTransferRecordEntity;
import org.example.backend.entity.DevicesEntity;
import org.example.backend.mapper.DeviceEventMapper;
import org.example.backend.mapper.DevicesMapper;
import org.example.backend.mapper.LabMapper;
import org.example.backend.service.BusinessLoopService;
import org.example.backend.service.DevicesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevicesServiceTransactionTest {
    @Mock
    private DevicesMapper devicesMapper;

    @Mock
    private DeviceEventMapper eventMapper;

    @Mock
    private LabMapper labMapper;

    @Mock
    private BusinessLoopService businessLoopService;

    @Test
    void transferReturnsRereadDeviceAndHistory() {
        DevicesEntity before = device(7L, "DEV-007", "调拨前设备");
        DevicesEntity after = device(7L, "DEV-007", "调拨后设备");
        DeviceTransferRecordEntity requestedTransfer = new DeviceTransferRecordEntity();
        requestedTransfer.setToLabId(9L);
        requestedTransfer.setToLocation("B201");
        DeviceTransferRecordEntity savedTransfer = new DeviceTransferRecordEntity();
        savedTransfer.setDeviceId(7L);
        List<DeviceTransferRecordEntity> history = List.of(savedTransfer);
        when(devicesMapper.getDevicesById(7L)).thenReturn(before, after);
        when(eventMapper.listTransferEvents(7L)).thenReturn(history);

        DevicesService.DeviceTransferRecordResult result = service().transferDevice(7L, requestedTransfer);

        assertSame(after, result.getDevice());
        assertSame(requestedTransfer, result.getRecord());
        assertSame(history, result.getRecords());
        InOrder order = inOrder(devicesMapper, eventMapper);
        order.verify(devicesMapper).getDevicesById(7L);
        order.verify(devicesMapper).updateTransferState(eq(7L), any(), any(), any());
        order.verify(eventMapper).insertTransferEvent(same(requestedTransfer));
        order.verify(devicesMapper).getDevicesById(7L);
        order.verify(eventMapper).listTransferEvents(7L);
    }

    @Test
    void inventoryReturnsRereadDeviceAndHistory() {
        DevicesEntity before = device(6L, "DEV-006", "盘点前设备");
        DevicesEntity after = device(6L, "DEV-006", "盘点后设备");
        DeviceInventoryRecordEntity requestedRecord = new DeviceInventoryRecordEntity();
        requestedRecord.setInspectedAt("2026-10-09 10:00:00");
        DeviceInventoryRecordEntity savedRecord = new DeviceInventoryRecordEntity();
        savedRecord.setDeviceId(6L);
        List<DeviceInventoryRecordEntity> history = List.of(savedRecord);
        when(devicesMapper.getDevicesById(6L)).thenReturn(before, after);
        when(eventMapper.listInventoryEvents(6L)).thenReturn(history);

        DevicesService.DeviceInventoryRecordResult result = service().recordDeviceInventory(6L, requestedRecord);

        assertSame(after, result.getDevice());
        assertSame(requestedRecord, result.getRecord());
        assertSame(history, result.getRecords());
        InOrder order = inOrder(devicesMapper, eventMapper);
        order.verify(devicesMapper).getDevicesById(6L);
        order.verify(eventMapper).insertInventoryEvent(same(requestedRecord));
        order.verify(devicesMapper).updateInventoryState(6L, "2026-10-09");
        order.verify(devicesMapper).getDevicesById(6L);
        order.verify(eventMapper).listInventoryEvents(6L);
    }

    @Test
    void springProxyExposesTransactionsForTransferAndInventory() throws Exception {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        AnnotationTransactionAttributeSource attributes = new AnnotationTransactionAttributeSource();
        ProxyFactory factory = new ProxyFactory(service());
        factory.setInterfaces(DevicesService.class);
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactionManager);
        interceptor.setTransactionAttributeSource(attributes);
        factory.addAdvice(interceptor);
        DevicesService proxy = (DevicesService) factory.getProxy();

        assertTrue(org.springframework.aop.support.AopUtils.isAopProxy(proxy));
        assertNotNull(attributes.getTransactionAttribute(
                DevicesServiceImpl.class.getMethod("transferDevice", Long.class, DeviceTransferRecordEntity.class),
                DevicesServiceImpl.class));
        assertNotNull(attributes.getTransactionAttribute(
                DevicesServiceImpl.class.getMethod("recordDeviceInventory", Long.class, DeviceInventoryRecordEntity.class),
                DevicesServiceImpl.class));

        DevicesEntity transferBefore = device(7L, "DEV-007", "调拨前设备");
        DevicesEntity transferAfter = device(7L, "DEV-007", "调拨后设备");
        when(devicesMapper.getDevicesById(7L)).thenReturn(transferBefore, transferAfter);
        when(eventMapper.listTransferEvents(7L)).thenReturn(List.of());
        proxy.transferDevice(7L, new DeviceTransferRecordEntity());
        verify(transactionManager).getTransaction(any());

        DevicesEntity inventoryBefore = device(6L, "DEV-006", "盘点前设备");
        DevicesEntity inventoryAfter = device(6L, "DEV-006", "盘点后设备");
        when(devicesMapper.getDevicesById(6L)).thenReturn(inventoryBefore, inventoryAfter);
        when(eventMapper.listInventoryEvents(6L)).thenReturn(List.of());
        proxy.recordDeviceInventory(6L, new DeviceInventoryRecordEntity());
        verify(transactionManager, org.mockito.Mockito.times(2)).getTransaction(any());
    }

    private DevicesServiceImpl service() {
        return new DevicesServiceImpl(devicesMapper, eventMapper, labMapper, businessLoopService);
    }

    private DevicesEntity device(Long id, String code, String name) {
        DevicesEntity device = new DevicesEntity();
        device.setId(id);
        device.setDeviceCode(code);
        device.setDeviceName(name);
        return device;
    }
}
