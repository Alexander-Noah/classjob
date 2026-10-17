/*
 * ManagerNameRow数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.entity;

public record ManagerNameRow(Long managerUserId, String managerName) {
}
