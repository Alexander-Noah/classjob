/*
 * 物联网能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.List;
import java.util.Map;

public interface IotHardwareService {
    /**
     * 获取概况。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getOverview();

    /**
     * 列出Hardware设备。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listHardwareDevices();

    /**
     * 获取实验室状态。
     * @param labId 实验室标识参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getLabStatus(Long labId);

    /**
     * 执行Access命令。
     * @param labId 实验室标识参数。
     * @param action action参数。
     * @param payload 载荷参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> executeAccessCommand(Long labId, String action, Map<String, Object> payload);

    /**
     * 获取实验室Camera。
     * @param labId 实验室标识参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getLabCamera(Long labId);

    /**
     * 获取设备状态。
     * @param code 编码参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getDeviceStatus(String code);

    /**
     * 执行设备命令。
     * @param code 编码参数。
     * @param action action参数。
     * @param payload 载荷参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> executeDeviceCommand(String code, String action, Map<String, Object> payload);

    /**
     * 获取Camera快照。
     * @param code 编码参数。
     * @return CameraSnapshot 类型的处理结果。
     */
    CameraSnapshot getCameraSnapshot(String code);

    record CameraSnapshot(byte[] bytes, String contentType) {
    }
}
