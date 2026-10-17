/*
 * 课表与时间占用能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.service.CapabilityResult;

public interface ScheduleOccupancyQuery {
    /**
     * 检查当前操作。
     * @param request 请求参数。
     * @return CapabilityResult<ScheduleOccupancyView> 类型的处理结果。
     */
    CapabilityResult<ScheduleOccupancyView> check(ScheduleOccupancyRequest request);
}
