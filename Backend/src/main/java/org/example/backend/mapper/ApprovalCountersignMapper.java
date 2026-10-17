/*
 * 审批会签的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/ApprovalCountersignMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.ApprovalCountersignEntity;

import java.util.List;

@Mapper
public interface ApprovalCountersignMapper {
    /**
     * 插入Countersign。
     * @param countersign countersign参数。
     * @return int 类型的处理结果。
     */
    int insertCountersign(ApprovalCountersignEntity countersign);

    /**
     * 获取Countersign。
     * @param id 标识参数。
     * @return ApprovalCountersignEntity 类型的处理结果。
     */
    ApprovalCountersignEntity getCountersign(@Param("id") Long id);

    /**
     * 列出Countersigns。
     * @param businessType business类型参数。
     * @param businessId business标识参数。
     * @param assigneeName assignee名称参数。
     * @param status 状态参数。
     * @return 当前操作得到的记录集合。
     */
    List<ApprovalCountersignEntity> listCountersigns(
            @Param("businessType") String businessType,
            @Param("businessId") String businessId,
            @Param("assigneeName") String assigneeName,
            @Param("status") String status
    );

    /**
     * 完成Countersign。
     * @param id 标识参数。
     * @param status 状态参数。
     * @param result 结果参数。
     * @param resultRemark 结果Remark参数。
     * @return int 类型的处理结果。
     */
    int completeCountersign(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("result") String result,
            @Param("resultRemark") String resultRemark
    );

    /**
     * 取消Countersign。
     * @param id 标识参数。
     * @param resultRemark 结果Remark参数。
     * @return int 类型的处理结果。
     */
    int cancelCountersign(@Param("id") Long id, @Param("resultRemark") String resultRemark);

    /**
     * 统计活动ByBusiness。
     * @param businessType business类型参数。
     * @param businessId business标识参数。
     * @return int 类型的处理结果。
     */
    int countActiveByBusiness(@Param("businessType") String businessType, @Param("businessId") String businessId);
}
