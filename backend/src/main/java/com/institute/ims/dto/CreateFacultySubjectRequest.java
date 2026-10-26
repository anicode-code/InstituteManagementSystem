package com.institute.ims.dto;

import jakarta.validation.constraints.NotNull;

public record CreateFacultySubjectRequest(
        @NotNull Long subjectId) {
}
