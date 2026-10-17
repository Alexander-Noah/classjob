/*
 * 实验室能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.LabEntity;

import java.util.List;

public interface LabService {
    /**
     * 获取实验室。
     * @return 当前操作得到的记录集合。
     */
    List<LabEntity> getLabs();

    /**
     * 获取实验室。
     * @param managerUserId manager用户标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<LabEntity> getLabs(Integer managerUserId);

    /**
     * 添加实验室。
     * @param lab 实验室参数。
     * @return LabEntity 类型的处理结果。
     */
    LabEntity addLab(LabEntity lab);

    /**
     * 获取实验室By标识。
     * @param id 标识参数。
     * @return Object 类型的处理结果。
     */
    Object getLabById(Integer id);

    /**
     * 更新实验室。
     * @param id 标识参数。
     * @param lab 实验室参数。
     */
    void updateLab(Integer id, LabEntity lab);

    /**
     * 更新实验室。
     * @param labs 实验室参数。
     * @return 当前操作得到的记录集合。
     */
    List<LabEntity> updateLabs(List<LabEntity> labs);

    /**
     * 删除实验室。
     * @param id 标识参数。
     */
    void deleteLab(Integer id);
}
