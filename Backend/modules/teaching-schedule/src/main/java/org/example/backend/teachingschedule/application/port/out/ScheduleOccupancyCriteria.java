package org.example.backend.teachingschedule.application.port.out;

public record ScheduleOccupancyCriteria(String semester, int academicWeek, String weekday) {
}
