/*
 * 实验室数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、labName；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.VO;

public class LabOptionVO {
    private Long id;
    private String labName;

    public LabOptionVO() {
    }

    public LabOptionVO(Long id, String labName) {
        this.id = id;
        this.labName = labName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabName() {
        return labName;
    }

    public void setLabName(String labName) {
        this.labName = labName;
    }
}
