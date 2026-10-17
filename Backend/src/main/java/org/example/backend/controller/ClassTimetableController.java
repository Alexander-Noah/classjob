/*
 * 全校课表采集的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/class-timetables/crawl、/class-timetables/crawler-config、/class-timetables/crawler-config、/class-timetables/crawler-tasks、/class-timetables/crawler-tasks；完整路径由类级与方法级映射组合。
 * 主要协作组件：ClassTimetableService。
 */
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

    /**
     * 构造 ClassTimetableController，初始化当前实例。
     * @param classTimetableService class课表服务参数。
     */
    public ClassTimetableController(ClassTimetableService classTimetableService) {
        this.classTimetableService = classTimetableService;
    }

    /** 同步执行一次采集直到服务返回；请求发出后并非立即返回后台任务 ID。 */
    @PostMapping("/class-timetables/crawl")
    public Result triggerCrawler() {
        log.info("触发课表爬虫脚本");
        return Result.success(classTimetableService.triggerCrawler());
    }

    /**
     * 获取Crawler配置。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/class-timetables/crawler-config")
    public Result getCrawlerConfig() {
        return Result.success(classTimetableService.getCrawlerConfig());
    }

    /** 保存后由服务重新注册定时计划，不通过控制器自行启动额外线程。 */
    @PostMapping("/class-timetables/crawler-config")
    public Result saveCrawlerConfig(@RequestBody Map<String, Object> payload) {
        log.info("保存课表定时抓取配置");
        return Result.success(classTimetableService.saveCrawlerConfig(payload));
    }

    /**
     * 列出CrawlerTasks。
     * @param limit 上限参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/class-timetables/crawler-tasks")
    public Result listCrawlerTasks(@RequestParam(required = false) Integer limit) {
        return Result.success(classTimetableService.listCrawlerTasks(limit));
    }

    /** 清除历史任务记录，不会取消当前 Python 进程或删除任务日志文件。 */
    @DeleteMapping("/class-timetables/crawler-tasks")
    public Result clearCrawlerTasks() {
        classTimetableService.clearCrawlerTasks();
        return Result.success(true);
    }

    /**
     * 获取Crawler凭据。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/class-timetables/credential")
    public Result getCrawlerCredential() {
        return Result.success(classTimetableService.getCrawlerCredential());
    }

    /**
     * 保存Crawler凭据。
     * @param payload 载荷参数。
     * @return Result 类型的处理结果。
     */
    @PostMapping("/class-timetables/credential")
    public Result saveCrawlerCredential(@RequestBody Map<String, String> payload) {
        log.info("保存课表爬虫教务账号");
        return Result.success(classTimetableService.saveCrawlerCredential(payload));
    }

    /**
     * 删除Crawler凭据。
     * @return Result 类型的处理结果。
     */
    @DeleteMapping("/class-timetables/credential")
    public Result deleteCrawlerCredential() {
        log.info("删除课表爬虫教务账号");
        classTimetableService.deleteCrawlerCredential();
        return Result.success(true);
    }
}
