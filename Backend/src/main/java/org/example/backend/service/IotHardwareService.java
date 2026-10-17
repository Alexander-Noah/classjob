/*
 * 物联网能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.List;
import java.util.Map;

public interface IotHardwareService {
    Map<String, Object> getOverview();

    List<Map<String, Object>> listHardwareDevices();

    Map<String, Object> getLabStatus(Long labId);

    Map<String, Object> executeAccessCommand(Long labId, String action, Map<String, Object> payload);

    Map<String, Object> getLabCamera(Long labId);

    Map<String, Object> getDeviceStatus(String code);

    Map<String, Object> executeDeviceCommand(String code, String action, Map<String, Object> payload);

    CameraSnapshot getCameraSnapshot(String code);

    record CameraSnapshot(byte[] bytes, String contentType) {
    }
}
