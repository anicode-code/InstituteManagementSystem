package com.institute.ims.dto;

import com.institute.ims.model.VerificationStatus;

public record AdminFacultyRosterItem(
        Long userId,
        String facultyId,
        String name,
        String profileImageUrl,
        String designation,
        String email,
        String phone,
        int assignedSubjects,
        int studentsTeaching,
        VerificationStatus status,
        Double averageStudentAttendance) {
}
