package com.institute.ims.controller;

import com.institute.ims.model.Assignment;
import com.institute.ims.model.AssignmentSubmission;
import com.institute.ims.model.User;
import com.institute.ims.service.AssignmentService;
import com.institute.ims.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService service;
    private final UserService userService;

    public AssignmentController(
            AssignmentService service,
            UserService userService) {

        this.service = service;
        this.userService = userService;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userService.getUserByRollNumber(authentication.getName());
    }

    @GetMapping
    public ResponseEntity<List<Assignment>> getAll(Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(service.getScopedAssignments(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Assignment> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/faculty/{facultyId}")
    public ResponseEntity<List<Assignment>> getByFaculty(
            @PathVariable Long facultyId) {

        return ResponseEntity.ok(service.getByFaculty(facultyId));
    }

    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<List<Assignment>> getBySubject(
            @PathVariable Long subjectId) {

        return ResponseEntity.ok(service.getBySubject(subjectId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN', 'HOD')")
    public ResponseEntity<Assignment> create(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        User currentUser = getAuthenticatedUser(authentication);

        String title = (String) body.get("title");
        String description = (String) body.get("description");
        LocalDate dueDate = LocalDate.parse((String) body.get("dueDate"));

        Integer maxMarks = body.get("maxMarks") != null
                ? Integer.parseInt(body.get("maxMarks").toString())
                : 100;

        Long subjectId = Long.parseLong(body.get("subjectId").toString());
        Long facultyId = body.get("facultyId") != null
                ? Long.parseLong(body.get("facultyId").toString())
                : currentUser.getId();

        return ResponseEntity.ok(
                service.create(title, description, dueDate, maxMarks, subjectId, facultyId, currentUser)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getAuthenticatedUser(authentication);
        service.delete(id, currentUser);

        return ResponseEntity.ok("Assignment deleted successfully");
    }

    // ──────── Submissions & Evaluation Endpoints ────────

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AssignmentSubmission> submit(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        User currentUser = getAuthenticatedUser(authentication);
        String text = body.get("submissionText");

        return ResponseEntity.ok(
                service.submitAssignment(id, currentUser.getId(), text)
        );
    }

    @GetMapping("/{id}/my-submission")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AssignmentSubmission> getMySubmission(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getAuthenticatedUser(authentication);
        return service.getMySubmission(id, currentUser.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}/submissions")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN', 'HOD')")
    public ResponseEntity<List<AssignmentSubmission>> getSubmissions(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(service.getSubmissionsForAssignment(id, currentUser));
    }

    @PostMapping("/submissions/{submissionId}/evaluate")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<AssignmentSubmission> evaluateSubmission(
            @PathVariable Long submissionId,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        User currentUser = getAuthenticatedUser(authentication);

        Integer marks = body.get("marks") != null
                ? Integer.parseInt(body.get("marks").toString())
                : null;
        String feedback = (String) body.get("feedback");

        return ResponseEntity.ok(
                service.evaluateSubmission(submissionId, marks, feedback, currentUser)
        );
    }
}
