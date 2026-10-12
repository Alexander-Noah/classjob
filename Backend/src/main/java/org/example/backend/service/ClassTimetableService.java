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
