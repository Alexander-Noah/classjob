/*
 * 环境模板的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/EnvironmentTemplateMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.VO.TeacherOptionVO;
import org.example.backend.entity.EnvironmentTemplateEntity;

import java.util.List;

@Mapper
public interface EnvironmentTemplateMapper {
     /**
      * 列出教师。
      * @return 当前操作得到的记录集合。
      */
     List<TeacherOptionVO> listTeachers();

    /**
     * 列出模板。
     * @return 当前操作得到的记录集合。
     */
    List<EnvironmentTemplateEntity> listTemplates();

    /**
     * 获取模板。
     * @param id 标识参数。
     * @return EnvironmentTemplateEntity 类型的处理结果。
     */
    EnvironmentTemplateEntity getTemplate(@Param("id") Long id);

    /**
     * 获取模板By课程。
     * @param course 课程参数。
     * @return EnvironmentTemplateEntity 类型的处理结果。
     */
    EnvironmentTemplateEntity getTemplateByCourse(@Param("course") String course);

    /**
     * 插入模板。
     * @param template 模板参数。
     * @return int 类型的处理结果。
     */
    int insertTemplate(EnvironmentTemplateEntity template);

    /**
     * 更新模板。
     * @param id 标识参数。
     * @param template 模板参数。
     * @return int 类型的处理结果。
     */
    int updateTemplate(@Param("id") Long id, @Param("template") EnvironmentTemplateEntity template);

    /**
     * 删除模板。
     * @param id 标识参数。
     * @return int 类型的处理结果。
     */
    int deleteTemplate(@Param("id") Long id);

    /**
     * 删除All模板。
     * @return int 类型的处理结果。
     */
    int deleteAllTemplates();


}
