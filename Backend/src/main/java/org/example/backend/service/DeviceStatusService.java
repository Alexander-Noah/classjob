/*
 * 设备运行状态能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.List;
import java.util.Map;

public interface DeviceStatusService {
    /**
     * 列出实验室运行时状态。
     * @param labId 实验室标识参数。
     * @param query 查询参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> listLabRuntimeStatus(Long labId, Map<String, String> query);

    /**
     * 获取设备运行时状态。
     * @param deviceId 设备标识参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getDeviceRuntimeStatus(Long deviceId);

    /**
     * 列出事件。
     * @param query 查询参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> listEvents(Map<String, String> query);

    /**
     * report运行时状态的处理逻辑。
     * @param payload 载荷参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> reportRuntimeStatus(Map<String, Object> payload);

    /**
     * 获取摘要。
     * @param query 查询参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getSummary(Map<String, String> query);
}
