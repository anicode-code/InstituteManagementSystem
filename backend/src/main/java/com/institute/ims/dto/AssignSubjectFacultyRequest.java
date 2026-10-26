package com.institute.ims.dto;

import jakarta.validation.constraints.NotNull;

public record AssignSubjectFacultyRequest(
        @NotNull Long facultyId) {
}
