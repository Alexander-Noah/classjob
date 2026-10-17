/*
 * 教学课表查询的 HTTP 接口入口，负责参数接收和响应适配。
 * 声明的路由片段：/class-timetables、/class-timetables/summary、/class-timetables/semesters；完整路径由类级与方法级映射组合。
 * 主要协作组件：TeachingScheduleQuery。
 */
package org.example.backend.controller;

import org.example.backend.result.Result;
import org.example.backend.service.ScheduleSearchCriteria;
import org.example.backend.service.TeachingScheduleQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class TeachingScheduleQueryController {
    private final TeachingScheduleQuery query;

    /**
     * 构造 TeachingScheduleQueryController，初始化当前实例。
     * @param query 查询参数。
     */
    public TeachingScheduleQueryController(TeachingScheduleQuery query) {
        this.query = query;
    }

    /**
     * 搜索当前操作。
     * @param semester 学期参数。
     * @param teacher 教师参数。
     * @param className class名称参数。
     * @param classroom classroom参数。
     * @param courseName 课程名称参数。
     * @param keyword keyword参数。
     * @param week 周次参数。
     * @param limit 上限参数。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/class-timetables")
    public Result search(
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String teacher,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String classroom,
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer week,
            @RequestParam(required = false) Integer limit
    ) {
        return Result.success(query.search(new ScheduleSearchCriteria(
                semester,
                teacher,
                className,
                classroom,
                courseName,
                keyword,
                week,
                limit
        )));
    }

    /**
     * 摘要的处理逻辑。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/class-timetables/summary")
    public Result summary() {
        return Result.success(query.summary());
    }

    /**
     * 学期的处理逻辑。
     * @return Result 类型的处理结果。
     */
    @GetMapping("/class-timetables/semesters")
    public Result semesters() {
        return Result.success(query.listSemesters());
    }
}
