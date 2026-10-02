package org.example.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.backend.result.Result;
import org.example.backend.service.ClassTimetableService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@CrossOrigin
@RestController
@Slf4j
public class ClassTimetableController {
    private final ClassTimetableService classTimetableService;

    public ClassTimetableController(ClassTimetableService classTimetableService) {
        this.classTimetableService = classTimetableService;
    }

    @PostMapping("/class-timetables/crawl")
    public Result triggerCrawler() {
        log.info("触发课表爬虫脚本");
        return Result.success(classTimetableService.triggerCrawler());
    }

    @GetMapping("/class-timetables/crawler-config")
    public Result getCrawlerConfig() {
        return Result.success(classTimetableService.getCrawlerConfig());
    }

    @PostMapping("/class-timetables/crawler-config")
    public Result saveCrawlerConfig(@RequestBody Map<String, Object> payload) {
        log.info("保存课表定时抓取配置");
        return Result.success(classTimetableService.saveCrawlerConfig(payload));
    }

    @GetMapping("/class-timetables/crawler-tasks")
    public Result listCrawlerTasks(@RequestParam(required = false) Integer limit) {
        return Result.success(classTimetableService.listCrawlerTasks(limit));
    }

    @DeleteMapping("/class-timetables/crawler-tasks")
    public Result clearCrawlerTasks() {
        classTimetableService.clearCrawlerTasks();
        return Result.success(true);
    }

    @GetMapping("/class-timetables/credential")
    public Result getCrawlerCredential() {
        return Result.success(classTimetableService.getCrawlerCredential());
    }

    @PostMapping("/class-timetables/credential")
    public Result saveCrawlerCredential(@RequestBody Map<String, String> payload) {
        log.info("保存课表爬虫教务账号");
        return Result.success(classTimetableService.saveCrawlerCredential(payload));
    }

    @DeleteMapping("/class-timetables/credential")
    public Result deleteCrawlerCredential() {
        log.info("删除课表爬虫教务账号");
        classTimetableService.deleteCrawlerCredential();
        return Result.success(true);
    }
}
