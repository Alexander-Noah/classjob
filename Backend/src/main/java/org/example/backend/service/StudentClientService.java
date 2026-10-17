/*
 * 学生客户端能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.StudentClientEntity;

import java.util.List;

public interface StudentClientService {
    StudentClientEntity online(StudentClientEntity client);

    void offline(StudentClientEntity client);

    List<StudentClientEntity> listByLabId(Long labId);
}
