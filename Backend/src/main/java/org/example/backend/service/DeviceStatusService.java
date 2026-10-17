/*
 * 设备运行状态能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.List;
import java.util.Map;

public interface DeviceStatusService {
    Map<String, Object> listLabRuntimeStatus(Long labId, Map<String, String> query);

    Map<String, Object> getDeviceRuntimeStatus(Long deviceId);

    Map<String, Object> listEvents(Map<String, String> query);

    Map<String, Object> reportRuntimeStatus(Map<String, Object> payload);

    Map<String, Object> getSummary(Map<String, String> query);
}
