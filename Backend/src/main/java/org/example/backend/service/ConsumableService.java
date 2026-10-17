/*
 * 耗材库存能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.ConsumableEntity;
import org.example.backend.entity.ConsumableStockRecordEntity;

import java.util.List;

public interface ConsumableService {
    /**
     * 列出Consumables。
     * @return 当前操作得到的记录集合。
     */
    List<ConsumableEntity> listConsumables();

    /**
     * 创建耗材库存。
     * @param consumable 耗材库存参数。
     * @return ConsumableEntity 类型的处理结果。
     */
    ConsumableEntity createConsumable(ConsumableEntity consumable);

    /**
     * 更新耗材库存。
     * @param id 标识参数。
     * @param consumable 耗材库存参数。
     * @return ConsumableEntity 类型的处理结果。
     */
    ConsumableEntity updateConsumable(Long id, ConsumableEntity consumable);

    /**
     * 删除耗材库存。
     * @param id 标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<ConsumableEntity> deleteConsumable(Long id);

    /**
     * replaceConsumables的处理逻辑。
     * @param consumables consumables参数。
     * @return 当前操作得到的记录集合。
     */
    List<ConsumableEntity> replaceConsumables(List<ConsumableEntity> consumables);

    /**
     * 重置Consumables。
     * @return 当前操作得到的记录集合。
     */
    List<ConsumableEntity> resetConsumables();

    /**
     * 列出库存记录集合。
     * @param consumableId consumable标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<ConsumableStockRecordEntity> listStockRecords(Long consumableId);

    /**
     * 记录ConsumableMovement。
     * @param id 标识参数。
     * @param record 记录参数。
     * @return ConsumableMovementResult 类型的处理结果。
     */
    ConsumableMovementResult recordConsumableMovement(Long id, ConsumableStockRecordEntity record);

    class ConsumableMovementResult {
        private ConsumableEntity consumable;
        private ConsumableStockRecordEntity record;
        private List<ConsumableStockRecordEntity> records;

        /**
         * 构造 ConsumableMovementResult，初始化当前实例。
         * @param consumable 耗材库存参数。
         * @param record 记录参数。
         * @param records 记录集合参数。
         */
        public ConsumableMovementResult(
                ConsumableEntity consumable,
                ConsumableStockRecordEntity record,
                List<ConsumableStockRecordEntity> records
        ) {
            this.consumable = consumable;
            this.record = record;
            this.records = records;
        }

        /**
         * 获取耗材库存。
         * @return ConsumableEntity 类型的处理结果。
         */
        public ConsumableEntity getConsumable() {
            return consumable;
        }

        /**
         * 设置耗材库存。
         * @param consumable 耗材库存参数。
         */
        public void setConsumable(ConsumableEntity consumable) {
            this.consumable = consumable;
        }

        /**
         * 获取记录。
         * @return ConsumableStockRecordEntity 类型的处理结果。
         */
        public ConsumableStockRecordEntity getRecord() {
            return record;
        }

        /**
         * 设置记录。
         * @param record 记录参数。
         */
        public void setRecord(ConsumableStockRecordEntity record) {
            this.record = record;
        }

        /**
         * 获取记录集合。
         * @return 当前操作得到的记录集合。
         */
        public List<ConsumableStockRecordEntity> getRecords() {
            return records;
        }

        /**
         * 设置记录集合。
         * @param records 记录集合参数。
         */
        public void setRecords(List<ConsumableStockRecordEntity> records) {
            this.records = records;
        }
    }
}
