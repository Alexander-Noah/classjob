/*
 * 实验室预约能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

public interface ReservationAvailabilityEvaluator {
    /**
     * 判断是否具有Conflict。
     * @param request 请求参数。
     * @return 当前判断的布尔结果。
     */
    boolean hasConflict(ReservationAvailabilityRequest request);
}
