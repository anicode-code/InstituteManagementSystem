package com.institute.ims.dto;

import com.institute.ims.model.VerificationStatus;
import com.institute.ims.model.Role;

import java.util.List;

public record HodFacultyRosterItem(
        Long id,
        String facultyId,
        Role role,
        String name,
        String profileImageUrl,
        String email,
        String phone,
        String designation,
        String qualification,
        String specialization,
        Double yearsOfExperience,
        String employmentType,
        String employmentStatus,
        VerificationStatus status,
        List<String> departments) {
}
