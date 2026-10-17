/*
 * 教师主机能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.TeacherHostEntity;

public interface TeacherHostService {
    /**
     * heartbeat的处理逻辑。
     * @param host 主机参数。
     * @return TeacherHostEntity 类型的处理结果。
     */
    TeacherHostEntity heartbeat(TeacherHostEntity host);

    /**
     * 当前主机的处理逻辑。
     * @param labId 实验室标识参数。
     * @return TeacherHostEntity 类型的处理结果。
     */
    TeacherHostEntity currentHost(Long labId);
}
