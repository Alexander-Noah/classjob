/*
 * 教务课表集成能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.AcademicScheduleCourse;
import org.example.backend.entity.AcademicScheduleImportRequest;
import org.example.backend.entity.CourseEnvironmentEntity;

import java.util.List;

public interface AcademicScheduleService {
    List<AcademicScheduleCourse> parseSchedule(AcademicScheduleImportRequest request);

    List<AcademicScheduleCourse> fetchSchedule(AcademicScheduleImportRequest request);

    List<CourseEnvironmentEntity> importSchedule(AcademicScheduleImportRequest request);
}
