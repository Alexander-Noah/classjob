/*
 * 教务课表集成能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.AcademicScheduleCourse;
import org.example.backend.entity.AcademicScheduleImportRequest;
import org.example.backend.entity.CourseEnvironmentEntity;

import java.util.List;

public interface AcademicScheduleService {
    /**
     * 解析课表。
     * @param request 请求参数。
     * @return 当前操作得到的记录集合。
     */
    List<AcademicScheduleCourse> parseSchedule(AcademicScheduleImportRequest request);

    /**
     * 获取课表。
     * @param request 请求参数。
     * @return 当前操作得到的记录集合。
     */
    List<AcademicScheduleCourse> fetchSchedule(AcademicScheduleImportRequest request);

    /**
     * 导入课表。
     * @param request 请求参数。
     * @return 当前操作得到的记录集合。
     */
    List<CourseEnvironmentEntity> importSchedule(AcademicScheduleImportRequest request);
}
