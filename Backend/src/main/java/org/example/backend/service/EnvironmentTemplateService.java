/*
 * 环境模板能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.VO.TeacherOptionVO;
import org.example.backend.entity.EnvironmentTemplateEntity;

import java.util.List;

public interface EnvironmentTemplateService {
    /**
     * 列出模板。
     * @return 当前操作得到的记录集合。
     */
    List<EnvironmentTemplateEntity> listTemplates();

    /**
     * 创建模板。
     * @param template 模板参数。
     * @return EnvironmentTemplateEntity 类型的处理结果。
     */
    EnvironmentTemplateEntity createTemplate(EnvironmentTemplateEntity template);

    /**
     * 更新模板。
     * @param id 标识参数。
     * @param template 模板参数。
     * @return EnvironmentTemplateEntity 类型的处理结果。
     */
    EnvironmentTemplateEntity updateTemplate(Long id, EnvironmentTemplateEntity template);

    /**
     * 删除模板。
     * @param id 标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<EnvironmentTemplateEntity> deleteTemplate(Long id);

    /**
     * replace模板的处理逻辑。
     * @param templates 模板参数。
     * @return 当前操作得到的记录集合。
     */
    List<EnvironmentTemplateEntity> replaceTemplates(List<EnvironmentTemplateEntity> templates);

    /**
     * 重置模板。
     * @return 当前操作得到的记录集合。
     */
    List<EnvironmentTemplateEntity> resetTemplates();

    /**
     * 列出教师。
     * @return 当前操作得到的记录集合。
     */
    List<TeacherOptionVO> listTeachers();

}
