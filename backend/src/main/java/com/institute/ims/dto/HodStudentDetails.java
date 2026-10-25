package com.institute.ims.dto;

import com.institute.ims.model.AttendanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HodStudentDetails(
        HodStudentRosterItem student,
        List<SubjectSummary> subjects,
        List<AttendanceDetail> attendance,
        List<AssignmentDetail> assignments) {

    public record SubjectSummary(
            Long id,
            String name,
            String code,
            Integer semester,
            Integer academicYear) {
    }

    public record AttendanceDetail(
            Long id,
            String subjectName,
            String subjectCode,
            LocalDate date,
            AttendanceStatus status,
            Integer performanceMarks) {
    }

    public record AssignmentDetail(
            Long id,
            String title,
            String description,
            String subjectName,
            String subjectCode,
            LocalDate dueDate,
            Integer maxMarks,
            boolean submitted,
            String submissionText,
            LocalDateTime submittedAt,
            String submissionStatus,
            Integer marks,
            String feedback) {
    }
}
