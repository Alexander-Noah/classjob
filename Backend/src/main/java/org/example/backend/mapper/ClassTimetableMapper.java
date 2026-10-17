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

    int deleteBySemester(@Param("semester") String semester);

    int insertTimetable(ClassTimetableEntity entity);

    Map<String, Object> getCrawlerConfig(@Param("configKey") String configKey);

    int upsertCrawlerConfig(
            @Param("configKey") String configKey,
            @Param("scheduleEnabled") Boolean scheduleEnabled,
            @Param("cronExpression") String cronExpression,
            @Param("semester") String semester,
            @Param("semesterStartDate") LocalDate semesterStartDate
    );

    List<Map<String, Object>> listCrawlerTasks(@Param("limit") Integer limit);

    int insertCrawlerTask(Map<String, Object> task);

    int updateCrawlerTask(Map<String, Object> task);

    int failInterruptedCrawlerTasks();

    int clearCrawlerTasks();

    Map<String, Object> getCrawlerCredential(@Param("credentialKey") String credentialKey);

    int upsertCrawlerCredential(
            @Param("credentialKey") String credentialKey,
            @Param("usernameCipher") String usernameCipher,
            @Param("passwordCipher") String passwordCipher
    );

    int deleteCrawlerCredential(@Param("credentialKey") String credentialKey);
}
