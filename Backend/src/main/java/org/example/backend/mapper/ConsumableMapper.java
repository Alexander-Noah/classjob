/*
 * 耗材库存的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/ConsumableMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.ConsumableEntity;
import org.example.backend.entity.ConsumableStockRecordEntity;

import java.util.List;

@Mapper
public interface ConsumableMapper {
    /**
     * 创建库存记录TableIfNotExists。
     */
    void createStockRecordTableIfNotExists();

    /**
     * 列出Consumables。
     * @return 当前操作得到的记录集合。
     */
    List<ConsumableEntity> listConsumables();

    /**
     * 获取耗材库存。
     * @param id 标识参数。
     * @return ConsumableEntity 类型的处理结果。
     */
    ConsumableEntity getConsumable(@Param("id") Long id);
    /**
     * 获取ConsumableForUpdate。
     * @param id 标识参数。
     * @return ConsumableEntity 类型的处理结果。
     */
    ConsumableEntity getConsumableForUpdate(@Param("id") Long id);
    /**
     * 统计借用记录集合。
     * @param consumableId consumable标识参数。
     * @return int 类型的处理结果。
     */
    int countLoanRecords(@Param("consumableId") Long consumableId);
    /**
     * 获取库存记录By来源。
     * @param source 来源参数。
     * @return ConsumableStockRecordEntity 类型的处理结果。
     */
    ConsumableStockRecordEntity getStockRecordBySource(@Param("source") String source);

    /**
     * 插入耗材库存。
     * @param consumable 耗材库存参数。
     * @return int 类型的处理结果。
     */
    int insertConsumable(ConsumableEntity consumable);

    /**
     * 更新耗材库存。
     * @param id 标识参数。
     * @param consumable 耗材库存参数。
     * @return int 类型的处理结果。
     */
    int updateConsumable(@Param("id") Long id, @Param("consumable") ConsumableEntity consumable);

    /**
     * 更新Consumable库存。
     * @param id 标识参数。
     * @param stock 库存参数。
     * @param status 状态参数。
     * @param tagType tag类型参数。
     * @return int 类型的处理结果。
     */
    int updateConsumableStock(
            @Param("id") Long id,
            @Param("stock") Integer stock,
            @Param("status") String status,
            @Param("tagType") String tagType
    );

    /**
     * 删除耗材库存。
     * @param id 标识参数。
     * @return int 类型的处理结果。
     */
    int deleteConsumable(@Param("id") Long id);

    /**
     * 删除AllConsumables。
     * @return int 类型的处理结果。
     */
    int deleteAllConsumables();

    /**
     * 插入库存记录。
     * @param record 记录参数。
     * @return int 类型的处理结果。
     */
    int insertStockRecord(ConsumableStockRecordEntity record);

    /**
     * 列出库存记录集合。
     * @param consumableId consumable标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<ConsumableStockRecordEntity> listStockRecords(@Param("consumableId") Long consumableId);
}
