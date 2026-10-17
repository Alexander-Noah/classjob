/*
 * 时间区间数据模型，用于表达字段结构，不在此处执行数据库操作。
 */
package org.example.backend.service;

import java.time.Instant;

/**
 * 使用绝对时间点传递区间，避免服务之间隐含依赖 JVM 默认时区。
 * 此 record 不执行构造校验；业务入口负责检查非空、起止顺序及是否允许跨日。
 *
 * @param startsAt 起点；从学校本地时间转换时使用 Asia/Shanghai
 * @param endsAt 终点；预约重叠 SQL 使用严格小于比较，首尾相接不视为重叠
 */
public record TimeRange(Instant startsAt, Instant endsAt) {
}
