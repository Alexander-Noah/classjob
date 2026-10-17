/*
 * 工作台统计能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> getOverview(Integer managerUserId);
}
