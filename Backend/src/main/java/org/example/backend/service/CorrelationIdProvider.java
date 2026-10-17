/*
 * 请求关联标识能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

public interface CorrelationIdProvider {
    String next();
}
