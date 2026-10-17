/*
 * 请求关联标识能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

public interface CorrelationIdProvider {
    /**
     * next的处理逻辑。
     * @return String 类型的处理结果。
     */
    String next();
}
