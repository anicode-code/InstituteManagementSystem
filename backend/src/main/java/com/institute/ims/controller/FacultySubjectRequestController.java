package com.institute.ims.controller;

import com.institute.ims.dto.CreateFacultySubjectRequest;
import com.institute.ims.dto.DepartmentSummaryResponse;
import com.institute.ims.dto.SubjectFacultyRequestResponse;
import com.institute.ims.dto.SubjectRequestDecision;
import com.institute.ims.dto.SubjectSummaryResponse;
import com.institute.ims.model.FacultySubjectRequestStatus;
import com.institute.ims.model.User;
import com.institute.ims.service.FacultySubjectRequestService;
import com.institute.ims.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subject-requests")
public class FacultySubjectRequestController {

    private final FacultySubjectRequestService requestService;
    private final UserService userService;

    public FacultySubjectRequestController(
            FacultySubjectRequestService requestService,
            UserService userService) {
        this.requestService = requestService;
        this.userService = userService;
    }

    @GetMapping("/eligible-subjects")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<List<SubjectSummaryResponse>> getEligibleSubjects(Authentication authentication) {
        return ResponseEntity.ok(requestService.getEligibleSubjects(currentUser(authentication)));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<List<SubjectFacultyRequestResponse>> getMyRequests(Authentication authentication) {
        return ResponseEntity.ok(requestService.getMyRequests(currentUser(authentication)));
    }

    @PostMapping
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<SubjectFacultyRequestResponse> create(
            @Valid @RequestBody CreateFacultySubjectRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                requestService.submitRequest(currentUser(authentication), request.subjectId()));
    }

    @GetMapping("/hod-departments")
    @PreAuthorize("hasRole('HOD')")
    public ResponseEntity<List<DepartmentSummaryResponse>> getHodDepartments(Authentication authentication) {
        return ResponseEntity.ok(requestService.getHodDepartments(currentUser(authentication)));
    }

    @GetMapping("/hod-subjects")
    @PreAuthorize("hasRole('HOD')")
    public ResponseEntity<List<SubjectSummaryResponse>> getHodSubjects(Authentication authentication) {
        return ResponseEntity.ok(requestService.getHodSubjects(currentUser(authentication)));
    }

    @GetMapping("/for-hod")
    @PreAuthorize("hasRole('HOD')")
    public ResponseEntity<List<SubjectFacultyRequestResponse>> getRequestsForHod(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) FacultySubjectRequestStatus status,
            Authentication authentication) {
        return ResponseEntity.ok(
                requestService.getHodRequests(currentUser(authentication), departmentId, status));
    }

    @PutMapping("/{requestId}/decision")
    @PreAuthorize("hasRole('HOD')")
    public ResponseEntity<SubjectFacultyRequestResponse> decide(
            @PathVariable Long requestId,
            @Valid @RequestBody SubjectRequestDecision decision,
            Authentication authentication) {
        return ResponseEntity.ok(requestService.decide(
                currentUser(authentication), requestId, decision.decision()));
    }

    private User currentUser(Authentication authentication) {
        return userService.getUserByRollNumber(authentication.getName());
    }
}
