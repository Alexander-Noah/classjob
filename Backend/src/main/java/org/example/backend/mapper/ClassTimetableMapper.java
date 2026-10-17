/*
 * 全校课表采集的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/ClassTimetableMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.ClassTimetableEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface ClassTimetableMapper {

    /**
     * 删除By学期。
     * @param semester 学期参数。
     * @return int 类型的处理结果。
     */
    int deleteBySemester(@Param("semester") String semester);

    /**
     * 插入课表。
     * @param entity entity参数。
     * @return int 类型的处理结果。
     */
    int insertTimetable(ClassTimetableEntity entity);

    /**
     * 获取Crawler配置。
     * @param configKey 配置键参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getCrawlerConfig(@Param("configKey") String configKey);

    /**
     * upsertCrawler配置的处理逻辑。
     * @param configKey 配置键参数。
     * @param scheduleEnabled 课表已启用参数。
     * @param cronExpression cronExpression参数。
     * @param semester 学期参数。
     * @param semesterStartDate 学期开始日期参数。
     * @return int 类型的处理结果。
     */
    int upsertCrawlerConfig(
            @Param("configKey") String configKey,
            @Param("scheduleEnabled") Boolean scheduleEnabled,
            @Param("cronExpression") String cronExpression,
            @Param("semester") String semester,
            @Param("semesterStartDate") LocalDate semesterStartDate
    );

    /**
     * 列出CrawlerTasks。
     * @param limit 上限参数。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listCrawlerTasks(@Param("limit") Integer limit);

    /**
     * 插入CrawlerTask。
     * @param task task参数。
     * @return int 类型的处理结果。
     */
    int insertCrawlerTask(Map<String, Object> task);

    /**
     * 更新CrawlerTask。
     * @param task task参数。
     * @return int 类型的处理结果。
     */
    int updateCrawlerTask(Map<String, Object> task);

    /**
     * failInterruptedCrawlerTasks的处理逻辑。
     * @return int 类型的处理结果。
     */
    int failInterruptedCrawlerTasks();

    /**
     * 清理CrawlerTasks。
     * @return int 类型的处理结果。
     */
    int clearCrawlerTasks();

    /**
     * 获取Crawler凭据。
     * @param credentialKey 凭据键参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getCrawlerCredential(@Param("credentialKey") String credentialKey);

    /**
     * upsertCrawler凭据的处理逻辑。
     * @param credentialKey 凭据键参数。
     * @param usernameCipher usernameCipher参数。
     * @param passwordCipher 密码Cipher参数。
     * @return int 类型的处理结果。
     */
    int upsertCrawlerCredential(
            @Param("credentialKey") String credentialKey,
            @Param("usernameCipher") String usernameCipher,
            @Param("passwordCipher") String passwordCipher
    );

    /**
     * 删除Crawler凭据。
     * @param credentialKey 凭据键参数。
     * @return int 类型的处理结果。
     */
    int deleteCrawlerCredential(@Param("credentialKey") String credentialKey);
}
