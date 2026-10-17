/*
 * 实验室数据模型，用于表达字段结构，不在此处执行数据库操作。
 * 主要字段：id、labName；JSON 别名和类型转换以字段注解为准。
 */
package org.example.backend.VO;

public class LabOptionVO {
    private Long id;
    private String labName;

    /**
     * 构造 LabOptionVO，初始化当前实例。
     */
    public LabOptionVO() {
    }

    /**
     * 构造 LabOptionVO，初始化当前实例。
     * @param id 标识参数。
     * @param labName 实验室名称参数。
     */
    public LabOptionVO(Long id, String labName) {
        this.id = id;
        this.labName = labName;
    }

    /**
     * 获取标识。
     * @return Long 类型的处理结果。
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置标识。
     * @param id 标识参数。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取实验室名称。
     * @return String 类型的处理结果。
     */
    public String getLabName() {
        return labName;
    }

    /**
     * 设置实验室名称。
     * @param labName 实验室名称参数。
     */
    public void setLabName(String labName) {
        this.labName = labName;
    }
}
