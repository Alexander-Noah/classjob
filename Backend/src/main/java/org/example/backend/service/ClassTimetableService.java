/*
 * 全校课表采集能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.AcademicCredentialView;

import java.util.List;
import java.util.Map;

public interface ClassTimetableService {
    Map<String, Object> triggerCrawler();

    Map<String, Object> getCrawlerConfig();

    Map<String, Object> saveCrawlerConfig(Map<String, Object> payload);

    List<Map<String, Object>> listCrawlerTasks(Integer limit);

    void clearCrawlerTasks();

    AcademicCredentialView getCrawlerCredential();

    AcademicCredentialView saveCrawlerCredential(Map<String, String> payload);

    void deleteCrawlerCredential();
}
