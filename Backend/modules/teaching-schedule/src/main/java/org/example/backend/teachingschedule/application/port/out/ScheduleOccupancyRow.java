package org.example.backend.teachingschedule.application.port.out;

public record ScheduleOccupancyRow(String classroom, Integer startSection, Integer endSection) {
}
