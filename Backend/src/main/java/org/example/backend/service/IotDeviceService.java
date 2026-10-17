/*
 * 设备管理能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.Map;

public interface IotDeviceService {
    /**
     * 列出设备。
     * @param query 查询参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> listDevices(Map<String, String> query);

    /**
     * 列出实验室设备。
     * @param labId 实验室标识参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> listLabDevices(Long labId);

    /**
     * 获取实验室概况。
     * @param labId 实验室标识参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getLabOverview(Long labId);

    /**
     * 创建设备。
     * @param payload 载荷参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> createDevice(Map<String, Object> payload);

    /**
     * 更新设备。
     * @param id 标识参数。
     * @param payload 载荷参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> updateDevice(Long id, Map<String, Object> payload);

    /**
     * 删除设备。
     * @param id 标识参数。
     */
    void deleteDevice(Long id);

    /**
     * control的处理逻辑。
     * @param payload 载荷参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> control(Map<String, Object> payload);

    /**
     * 列出命令Logs。
     * @param query 查询参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> listCommandLogs(Map<String, String> query);

    /**
     * 获取环境。
     * @param labId 实验室标识参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getEnvironment(Long labId);
}
