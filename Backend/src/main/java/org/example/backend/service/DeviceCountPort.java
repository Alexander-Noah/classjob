/*
 * 设备管理能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.Map;
import java.util.Set;

public interface DeviceCountPort {
    Map<Long, Integer> countByLabIds(Set<Long> labIds);
}
