/*
 * 实验室数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

public record LabAccessScope(Long managerUserId) {
    public static LabAccessScope all() {
        return new LabAccessScope(null);
    }

    public static LabAccessScope managedBy(long managerUserId) {
        return new LabAccessScope(managerUserId);
    }
}
