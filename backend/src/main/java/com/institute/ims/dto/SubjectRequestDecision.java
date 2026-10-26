package com.institute.ims.dto;

import com.institute.ims.model.FacultySubjectRequestStatus;
import jakarta.validation.constraints.NotNull;

public record SubjectRequestDecision(
        @NotNull FacultySubjectRequestStatus decision) {
}
