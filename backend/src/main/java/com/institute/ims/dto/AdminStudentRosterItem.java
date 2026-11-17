package com.institute.ims.dto;

import com.institute.ims.model.VerificationStatus;

import java.time.LocalDateTime;

public record AdminStudentRosterItem(
        Long id,
        String name,
        String photoUrl,
        String rollNumber,
        String email,
        Integer year,
        Integer semester,
        VerificationStatus status,
        LocalDateTime createdAt,
        Double attendancePercentage,
        Double academicPerformance,
        long submittedAssignments,
        long totalAssignments,
        long pendingAssignments,
        boolean lowAttendance,
        boolean poorPerformance,
        boolean decliningPerformance,
        boolean requiresAttention) {
}
