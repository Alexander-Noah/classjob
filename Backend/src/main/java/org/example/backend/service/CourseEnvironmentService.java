/*
 * 课程环境能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.CourseEnvironmentEntity;

import java.util.List;

public interface CourseEnvironmentService {
    List<CourseEnvironmentEntity> getEnvironment();

    Object InserterCourseEnvironment(CourseEnvironmentEntity courseEnvironment);

    Object getCourseEnvironment(Integer id);

    Object updateCourseEnvironment(Integer id, CourseEnvironmentEntity courseEnvironment);

    Object deleteCourseEnvironment(Integer id);

    Object confirmCourseEnvironment(Integer id);
}
