/*
 * 设备管理能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.Map;
import java.util.Set;

public interface DeviceCountPort {
    /**
     * 统计By实验室标识集合。
     * @param labIds 实验室标识集合参数。
     * @return 当前操作返回的键值结果。
     */
    Map<Long, Integer> countByLabIds(Set<Long> labIds);
}
