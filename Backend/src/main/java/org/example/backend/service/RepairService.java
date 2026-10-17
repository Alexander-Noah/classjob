/*
 * 报修能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.RepairEntity;

import java.util.List;

public interface RepairService {
    /**
     * 列出Repairs。
     * @return 当前操作得到的记录集合。
     */
    List<RepairEntity> listRepairs();

    /**
     * 创建报修。
     * @param repair 报修参数。
     * @return RepairEntity 类型的处理结果。
     */
    RepairEntity createRepair(RepairEntity repair);

    /**
     * 更新报修。
     * @param id 标识参数。
     * @param repair 报修参数。
     * @return RepairEntity 类型的处理结果。
     */
    RepairEntity updateRepair(String id, RepairEntity repair);

    /**
     * 删除报修。
     * @param id 标识参数。
     */
    void deleteRepair(String id);

    /**
     * replaceRepairs的处理逻辑。
     * @param repairs repairs参数。
     * @return 当前操作得到的记录集合。
     */
    List<RepairEntity> replaceRepairs(List<RepairEntity> repairs);

    /**
     * 重置Repairs。
     * @return 当前操作得到的记录集合。
     */
    List<RepairEntity> resetRepairs();

    /**
     * patchRepair的处理逻辑。
     * @param id 标识参数。
     * @param repair 报修参数。
     * @param defaultStatus 默认状态参数。
     * @param defaultProgress 默认Progress参数。
     * @param defaultResult 默认结果参数。
     * @return RepairEntity 类型的处理结果。
     */
    RepairEntity patchRepair(String id, RepairEntity repair, String defaultStatus, Integer defaultProgress, String defaultResult);
}
