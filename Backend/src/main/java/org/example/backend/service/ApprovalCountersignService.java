/*
 * 审批会签能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.ApprovalCountersignEntity;

import java.util.List;

public interface ApprovalCountersignService {
    List<ApprovalCountersignEntity> listCountersigns(String businessType, String businessId, String assigneeName, String status);

    ApprovalCountersignEntity createCountersign(ApprovalCountersignEntity countersign);

    ApprovalCountersignEntity completeCountersign(Long id, ApprovalCountersignEntity payload);

    ApprovalCountersignEntity cancelCountersign(Long id, ApprovalCountersignEntity payload);
}
