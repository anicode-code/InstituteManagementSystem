package com.institute.ims.dto;

import com.institute.ims.model.VerificationStatus;

import java.util.List;

public record AdminFacultyRosterPage(
        List<AdminFacultyRosterItem> items,
        Summary summary,
        FilterOptions filters,
        int page,
        int size,
        int totalElements,
        int totalPages) {

    public record Summary(
            int totalFacultyMembers,
            int activeFaculty,
            int totalSubjectsAssigned,
            Double averageStudentAttendance) {
    }

    public record FilterOptions(
            List<SubjectOption> subjects,
            List<Integer> academicYears,
            List<VerificationStatus> statuses) {
    }

    public record SubjectOption(
            Long id,
            String name,
            String code,
            Integer academicYear,
            Integer semester) {
    }
}
