/*
 * 课表与时间占用能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.service.CapabilityResult;

public interface ScheduleSnapshotPort {
    /**
     * 查找ByTeachingTask标识。
     * @param teachingTaskId teachingTask标识参数。
     * @return CapabilityResult<ScheduleSnapshot> 类型的处理结果。
     */
    CapabilityResult<ScheduleSnapshot> findByTeachingTaskId(Long teachingTaskId);
}
