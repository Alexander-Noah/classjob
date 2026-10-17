/*
 * 使用记录能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.UsageRecordEntity;

import java.util.List;

public interface UsageRecordService {
    List<UsageRecordEntity> listUsageRecords();

    List<UsageRecordEntity> replaceUsageRecords(List<UsageRecordEntity> records);

    List<UsageRecordEntity> resetUsageRecords();

    UsageRecordEntity updateStatus(Long id, String status);
}
