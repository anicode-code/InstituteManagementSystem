package com.institute.ims.dto;

import java.util.List;

public record AdminStudentRosterPage(
        List<AdminStudentRosterItem> students,
        Summary summary,
        int page,
        int size,
        long totalElements,
        int totalPages,
        double attendanceThreshold,
        double performanceThreshold) {

    public record Summary(
            long totalStudents,
            Double averageAttendance,
            Double averageAcademicPerformance,
            long studentsRequiringAttention) {
    }
}
