/*
 * 使用记录数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、person、resource、scene、useTime、status；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class UsageRecordEntity {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Long id;
    private String person;
    private String resource;
    private String scene;
    @JsonAlias("time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime useTime;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String recordNo;
    private String role;
    private String type;
    private String lab;
    private String leaveTime;
    private String duration;
    private String accessResult;
    private String reviewer;
    private String abnormalReason;
    private String note;
    private String sourceType;
    private Boolean readonly;

    /**
     * 获取时间。
     * @return String 类型的处理结果。
     */
    public String getTime() {
        return useTime == null ? "" : useTime.format(TIME_FORMATTER);
    }

    /**
     * 获取Tag类型。
     * @return String 类型的处理结果。
     */
    public String getTagType() {
        if ("正常".equals(status) || "已归档".equals(status)) return "success";
        if ("异常".equals(status)) return "danger";
        if ("待复核".equals(status)) return "warning";
        return "primary";
    }
}
