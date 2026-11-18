package com.institute.ims.dto;

public record AttachmentFile(
        String fileName,
        String contentType,
        byte[] data) {
}
