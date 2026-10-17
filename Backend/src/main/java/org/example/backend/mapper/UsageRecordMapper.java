/*
 * 使用记录的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/UsageRecordMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.UsageRecordEntity;

import java.util.List;

@Mapper
public interface UsageRecordMapper {
    /**
     * 列出Usage记录集合。
     * @return 当前操作得到的记录集合。
     */
    List<UsageRecordEntity> listUsageRecords();

    /**
     * 获取Usage记录。
     * @param id 标识参数。
     * @return UsageRecordEntity 类型的处理结果。
     */
    UsageRecordEntity getUsageRecord(@Param("id") Long id);

    /**
     * 插入Usage记录。
     * @param usageRecord 使用记录参数。
     * @return int 类型的处理结果。
     */
    int insertUsageRecord(UsageRecordEntity usageRecord);

    /**
     * 统计Usage记录BySignature。
     * @param person person参数。
     * @param resource 资源参数。
     * @param scene scene参数。
     * @param useTime use时间参数。
     * @return int 类型的处理结果。
     */
    int countUsageRecordBySignature(
            @Param("person") String person,
            @Param("resource") String resource,
            @Param("scene") String scene,
            @Param("useTime") java.time.LocalDateTime useTime
    );

    /**
     * 更新Usage记录。
     * @param id 标识参数。
     * @param usageRecord 使用记录参数。
     * @return int 类型的处理结果。
     */
    int updateUsageRecord(@Param("id") Long id, @Param("record") UsageRecordEntity usageRecord);

    /**
     * 更新Usage记录状态。
     * @param id 标识参数。
     * @param status 状态参数。
     * @return int 类型的处理结果。
     */
    int updateUsageRecordStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 删除AllUsage记录集合。
     */
    void deleteAllUsageRecords();
}
