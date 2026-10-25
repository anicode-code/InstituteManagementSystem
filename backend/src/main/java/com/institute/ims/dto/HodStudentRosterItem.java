package com.institute.ims.dto;

import com.institute.ims.model.VerificationStatus;

import java.time.LocalDateTime;

public record HodStudentRosterItem(
        Long id,
        String name,
        String rollNumber,
        String email,
        VerificationStatus status,
        Integer year,
        Integer semester,
        LocalDateTime createdAt,
        DepartmentSummary department,
        Double attendancePercentage,
        Double classPerformance,
        long pendingAssignments) {

    public record DepartmentSummary(Long id, String name, String code) {
    }
}
