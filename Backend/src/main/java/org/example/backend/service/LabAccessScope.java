/*
 * 实验室数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

public record LabAccessScope(Long managerUserId) {
    /**
     * all的处理逻辑。
     * @return LabAccessScope 类型的处理结果。
     */
    public static LabAccessScope all() {
        return new LabAccessScope(null);
    }

    /**
     * managedBy的处理逻辑。
     * @param managerUserId manager用户标识参数。
     * @return LabAccessScope 类型的处理结果。
     */
    public static LabAccessScope managedBy(long managerUserId) {
        return new LabAccessScope(managerUserId);
    }
}
