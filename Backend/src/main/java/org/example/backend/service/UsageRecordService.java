/*
 * 使用记录能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.UsageRecordEntity;

import java.util.List;

public interface UsageRecordService {
    /**
     * 列出Usage记录集合。
     * @return 当前操作得到的记录集合。
     */
    List<UsageRecordEntity> listUsageRecords();

    /**
     * replaceUsage记录集合的处理逻辑。
     * @param records 记录集合参数。
     * @return 当前操作得到的记录集合。
     */
    List<UsageRecordEntity> replaceUsageRecords(List<UsageRecordEntity> records);

    /**
     * 重置Usage记录集合。
     * @return 当前操作得到的记录集合。
     */
    List<UsageRecordEntity> resetUsageRecords();

    /**
     * 更新状态。
     * @param id 标识参数。
     * @param status 状态参数。
     * @return UsageRecordEntity 类型的处理结果。
     */
    UsageRecordEntity updateStatus(Long id, String status);
}
