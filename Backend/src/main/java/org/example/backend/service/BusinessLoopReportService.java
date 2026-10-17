/*
 * 业务闭环能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.Map;

public interface BusinessLoopReportService {
    /**
     * 获取概况。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getOverview();
}
