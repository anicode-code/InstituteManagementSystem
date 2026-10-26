package com.institute.ims.dto;

import com.institute.ims.model.Subject;

import java.util.List;

public record SubjectSummaryResponse(
        Long id,
        String name,
        String code,
        Long departmentId,
        String departmentName,
        Integer semester,
        Integer academicYear,
        List<FacultySummaryResponse> faculty) {

    public static SubjectSummaryResponse from(Subject subject) {
        var department = subject.getDepartment();
        return new SubjectSummaryResponse(
                subject.getId(),
                subject.getName(),
                subject.getCode(),
                department == null ? null : department.getId(),
                department == null ? null : department.getName(),
                subject.getSemester(),
                subject.getAcademicYear(),
                subject.getFacultyMembers().stream()
                        .map(FacultySummaryResponse::from)
                        .toList());
    }
}
