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
    int insertCountersign(ApprovalCountersignEntity countersign);

    ApprovalCountersignEntity getCountersign(@Param("id") Long id);

    List<ApprovalCountersignEntity> listCountersigns(
            @Param("businessType") String businessType,
            @Param("businessId") String businessId,
            @Param("assigneeName") String assigneeName,
            @Param("status") String status
    );

    int completeCountersign(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("result") String result,
            @Param("resultRemark") String resultRemark
    );

    int cancelCountersign(@Param("id") Long id, @Param("resultRemark") String resultRemark);

    int countActiveByBusiness(@Param("businessType") String businessType, @Param("businessId") String businessId);
}
