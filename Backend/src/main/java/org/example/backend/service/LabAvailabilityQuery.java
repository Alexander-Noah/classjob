/*
 * 实验室能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.service.CapabilityResult;

import java.time.Instant;

public interface LabAvailabilityQuery {
    /**
     * 检查当前操作。
     * @param labId 实验室标识参数。
     * @param expectedCapacity 预期Capacity参数。
     * @param startsAt 开始At参数。
     * @param endsAt 结束At参数。
     * @return CapabilityResult<LabAvailabilityView> 类型的处理结果。
     */
    CapabilityResult<LabAvailabilityView> check(Long labId, int expectedCapacity, Instant startsAt, Instant endsAt);
}
