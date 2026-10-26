package com.institute.ims.dto;

import com.institute.ims.model.User;

public record FacultySummaryResponse(
        Long id,
        String name,
        String rollNumber,
        String email,
        String role) {

    public static FacultySummaryResponse from(User user) {
        return new FacultySummaryResponse(
                user.getId(),
                user.getName(),
                user.getRollNumber(),
                user.getEmail(),
                user.getRole().name());
    }
}
