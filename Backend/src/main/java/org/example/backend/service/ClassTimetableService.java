/*
 * 全校课表采集能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.AcademicCredentialView;

import java.util.List;
import java.util.Map;

public interface ClassTimetableService {
    /**
     * triggerCrawler的处理逻辑。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> triggerCrawler();

    /**
     * 获取Crawler配置。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getCrawlerConfig();

    /**
     * 保存Crawler配置。
     * @param payload 载荷参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> saveCrawlerConfig(Map<String, Object> payload);

    /**
     * 列出CrawlerTasks。
     * @param limit 上限参数。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listCrawlerTasks(Integer limit);

    /**
     * 清理CrawlerTasks。
     */
    void clearCrawlerTasks();

    /**
     * 获取Crawler凭据。
     * @return AcademicCredentialView 类型的处理结果。
     */
    AcademicCredentialView getCrawlerCredential();

    /**
     * 保存Crawler凭据。
     * @param payload 载荷参数。
     * @return AcademicCredentialView 类型的处理结果。
     */
    AcademicCredentialView saveCrawlerCredential(Map<String, String> payload);

    /**
     * 删除Crawler凭据。
     */
    void deleteCrawlerCredential();
}
