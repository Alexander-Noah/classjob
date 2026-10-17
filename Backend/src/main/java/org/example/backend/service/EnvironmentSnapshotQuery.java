/*
 * 实验环境能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.service.CapabilityResult;

public interface EnvironmentSnapshotQuery {
    /**
     * 查找ByTeachingTask标识。
     * @param teachingTaskId teachingTask标识参数。
     * @return CapabilityResult<EnvironmentSnapshotView> 类型的处理结果。
     */
    CapabilityResult<EnvironmentSnapshotView> findByTeachingTaskId(Long teachingTaskId);
}
