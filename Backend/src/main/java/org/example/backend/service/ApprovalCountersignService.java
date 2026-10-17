/*
 * 审批会签能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.ApprovalCountersignEntity;

import java.util.List;

public interface ApprovalCountersignService {
    /**
     * 列出Countersigns。
     * @param businessType business类型参数。
     * @param businessId business标识参数。
     * @param assigneeName assignee名称参数。
     * @param status 状态参数。
     * @return 当前操作得到的记录集合。
     */
    List<ApprovalCountersignEntity> listCountersigns(String businessType, String businessId, String assigneeName, String status);

    /**
     * 创建Countersign。
     * @param countersign countersign参数。
     * @return ApprovalCountersignEntity 类型的处理结果。
     */
    ApprovalCountersignEntity createCountersign(ApprovalCountersignEntity countersign);

    /**
     * 完成Countersign。
     * @param id 标识参数。
     * @param payload 载荷参数。
     * @return ApprovalCountersignEntity 类型的处理结果。
     */
    ApprovalCountersignEntity completeCountersign(Long id, ApprovalCountersignEntity payload);

    /**
     * 取消Countersign。
     * @param id 标识参数。
     * @param payload 载荷参数。
     * @return ApprovalCountersignEntity 类型的处理结果。
     */
    ApprovalCountersignEntity cancelCountersign(Long id, ApprovalCountersignEntity payload);
}
