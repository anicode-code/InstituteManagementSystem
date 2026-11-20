package com.institute.ims.dto;

import java.time.LocalDateTime;

public record StudyMaterialResponse(
        Long id,
        String title,
        String description,
        String fileName,
        String contentType,
        long fileSize,
        Long subjectId,
        String subjectName,
        String subjectCode,
        Long uploadedById,
        String uploadedByName,
        LocalDateTime uploadedAt) {
}
