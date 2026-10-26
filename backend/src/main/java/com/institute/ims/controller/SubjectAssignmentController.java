package com.institute.ims.controller;

import com.institute.ims.dto.AssignSubjectFacultyRequest;
import com.institute.ims.dto.FacultySummaryResponse;
import com.institute.ims.model.User;
import com.institute.ims.service.SubjectAssignmentService;
import com.institute.ims.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subject-assignments")
@PreAuthorize("hasRole('HOD')")
public class SubjectAssignmentController {

    private final SubjectAssignmentService assignmentService;
    private final UserService userService;

    public SubjectAssignmentController(
            SubjectAssignmentService assignmentService,
            UserService userService) {
        this.assignmentService = assignmentService;
        this.userService = userService;
    }

    @GetMapping("/subjects/{subjectId}/faculty")
    public ResponseEntity<List<FacultySummaryResponse>> getAssignedFaculty(
            @PathVariable Long subjectId, Authentication authentication) {
        return ResponseEntity.ok(assignmentService.getAssignedFaculty(currentUser(authentication), subjectId));
    }

    @GetMapping("/subjects/{subjectId}/eligible-faculty")
    public ResponseEntity<List<FacultySummaryResponse>> getEligibleFaculty(
            @PathVariable Long subjectId, Authentication authentication) {
        return ResponseEntity.ok(assignmentService.getEligibleFaculty(currentUser(authentication), subjectId));
    }

    @PostMapping("/subjects/{subjectId}/faculty")
    public ResponseEntity<List<FacultySummaryResponse>> assign(
            @PathVariable Long subjectId,
            @Valid @RequestBody AssignSubjectFacultyRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(assignmentService.assign(
                currentUser(authentication), subjectId, request.facultyId()));
    }

    @DeleteMapping("/subjects/{subjectId}/faculty/{facultyId}")
    public ResponseEntity<List<FacultySummaryResponse>> remove(
            @PathVariable Long subjectId,
            @PathVariable Long facultyId,
            Authentication authentication) {
        return ResponseEntity.ok(assignmentService.remove(
                currentUser(authentication), subjectId, facultyId));
    }

    private User currentUser(Authentication authentication) {
        return userService.getUserByRollNumber(authentication.getName());
    }
}
