package com.institute.ims.dto;

public record AttachmentMetadata(
        Long id,
        String fileName,
        String contentType,
        long fileSize) {
}
