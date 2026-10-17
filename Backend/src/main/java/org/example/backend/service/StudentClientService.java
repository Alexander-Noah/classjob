/*
 * 学生客户端能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.StudentClientEntity;

import java.util.List;

public interface StudentClientService {
    /**
     * online的处理逻辑。
     * @param client 客户端参数。
     * @return StudentClientEntity 类型的处理结果。
     */
    StudentClientEntity online(StudentClientEntity client);

    /**
     * offline的处理逻辑。
     * @param client 客户端参数。
     */
    void offline(StudentClientEntity client);

    /**
     * 列出By实验室标识。
     * @param labId 实验室标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<StudentClientEntity> listByLabId(Long labId);
}
