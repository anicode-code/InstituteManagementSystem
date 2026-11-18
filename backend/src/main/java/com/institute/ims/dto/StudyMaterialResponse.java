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
        String uploadedByName,
        LocalDateTime uploadedAt) {
}
