/*
 * 课程环境的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/CourseEnvironmentMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.CourseEnvironmentEntity;

import java.util.List;

@Mapper
public interface CourseEnvironmentMapper {
    /**
     * 获取环境。
     * @return 当前操作得到的记录集合。
     */
    List<CourseEnvironmentEntity> getEnvironment();

    /**
     * Inserter课程环境的处理逻辑。
     * @param courseEnvironment 课程环境参数。
     * @return int 类型的处理结果。
     */
    int InserterCourseEnvironment(CourseEnvironmentEntity courseEnvironment);

    /**
     * 获取课程环境。
     * @param id 标识参数。
     * @return CourseEnvironmentEntity 类型的处理结果。
     */
    CourseEnvironmentEntity getCourseEnvironment(@Param("id") Integer id);

    /**
     * 更新课程环境。
     * @param id 标识参数。
     * @param courseEnvironment 课程环境参数。
     * @return int 类型的处理结果。
     */
    int updateCourseEnvironment(@Param("id") Integer id, @Param("courseEnvironment") CourseEnvironmentEntity courseEnvironment);

    /**
     * 删除课程环境。
     * @param id 标识参数。
     * @return int 类型的处理结果。
     */
    int deleteCourseEnvironment(@Param("id") Integer id);

    /**
     * confirm课程环境的处理逻辑。
     * @param id 标识参数。
     * @return int 类型的处理结果。
     */
    int confirmCourseEnvironment(@Param("id") Integer id);

    /**
     * 删除All课程环境。
     * @return int 类型的处理结果。
     */
    int deleteAllCourseEnvironments();
}
