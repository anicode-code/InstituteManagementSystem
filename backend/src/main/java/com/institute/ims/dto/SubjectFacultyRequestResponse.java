package com.institute.ims.dto;

import com.institute.ims.model.FacultySubjectRequest;

import java.time.LocalDateTime;

public record SubjectFacultyRequestResponse(
        Long id,
        Long facultyId,
        String facultyName,
        String facultyRollNumber,
        Long subjectId,
        String subjectName,
        String subjectCode,
        Long departmentId,
        String departmentName,
        Integer semester,
        Integer academicYear,
        String status,
        Long reviewedById,
        String reviewedByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime reviewedAt) {

    public static SubjectFacultyRequestResponse from(FacultySubjectRequest request) {
        var faculty = request.getFaculty();
        var subject = request.getSubject();
        var department = subject.getDepartment();
        var reviewer = request.getReviewedBy();
        return new SubjectFacultyRequestResponse(
                request.getId(),
                faculty.getId(),
                faculty.getName(),
                faculty.getRollNumber(),
                subject.getId(),
                subject.getName(),
                subject.getCode(),
                department == null ? null : department.getId(),
                department == null ? null : department.getName(),
                subject.getSemester(),
                subject.getAcademicYear(),
                request.getStatus().name(),
                reviewer == null ? null : reviewer.getId(),
                reviewer == null ? null : reviewer.getName(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.getReviewedAt());
    }
}
