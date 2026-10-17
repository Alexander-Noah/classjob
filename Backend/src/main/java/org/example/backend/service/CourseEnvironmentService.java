/*
 * 课程环境能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.CourseEnvironmentEntity;

import java.util.List;

public interface CourseEnvironmentService {
    /**
     * 获取环境。
     * @return 当前操作得到的记录集合。
     */
    List<CourseEnvironmentEntity> getEnvironment();

    /**
     * Inserter课程环境的处理逻辑。
     * @param courseEnvironment 课程环境参数。
     * @return Object 类型的处理结果。
     */
    Object InserterCourseEnvironment(CourseEnvironmentEntity courseEnvironment);

    /**
     * 获取课程环境。
     * @param id 标识参数。
     * @return Object 类型的处理结果。
     */
    Object getCourseEnvironment(Integer id);

    /**
     * 更新课程环境。
     * @param id 标识参数。
     * @param courseEnvironment 课程环境参数。
     * @return Object 类型的处理结果。
     */
    Object updateCourseEnvironment(Integer id, CourseEnvironmentEntity courseEnvironment);

    /**
     * 删除课程环境。
     * @param id 标识参数。
     * @return Object 类型的处理结果。
     */
    Object deleteCourseEnvironment(Integer id);

    /**
     * confirm课程环境的处理逻辑。
     * @param id 标识参数。
     * @return Object 类型的处理结果。
     */
    Object confirmCourseEnvironment(Integer id);
}
