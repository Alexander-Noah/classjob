package org.example.backend.teachingschedule.web;

import org.example.backend.result.Result;
import org.example.backend.teachingschedule.api.ScheduleSearchCriteria;
import org.example.backend.teachingschedule.api.TeachingScheduleQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class TeachingScheduleQueryController {
    private final TeachingScheduleQuery query;

    public TeachingScheduleQueryController(TeachingScheduleQuery query) {
        this.query = query;
    }

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

    @GetMapping("/class-timetables/summary")
    public Result summary() {
        return Result.success(query.summary());
    }

    @GetMapping("/class-timetables/semesters")
    public Result semesters() {
        return Result.success(query.listSemesters());
    }
}
