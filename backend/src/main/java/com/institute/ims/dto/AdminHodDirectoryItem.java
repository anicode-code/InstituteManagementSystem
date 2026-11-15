package com.institute.ims.dto;

import com.institute.ims.model.VerificationStatus;

public record AdminHodDirectoryItem(
        Long userId,
        String hodId,
        String name,
        String profileImageUrl,
        String designation,
        String email,
        String phone,
        String departmentName,
        String departmentCode,
        String appointmentDate,
        VerificationStatus status) {
}
