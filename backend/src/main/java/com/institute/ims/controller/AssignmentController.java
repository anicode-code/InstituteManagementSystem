package com.institute.ims.controller;

import com.institute.ims.model.Assignment;
import com.institute.ims.model.AssignmentSubmission;
import com.institute.ims.model.User;
import com.institute.ims.dto.AttachmentFile;
import com.institute.ims.dto.AttachmentMetadata;
import com.institute.ims.service.AssignmentService;
import com.institute.ims.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

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

    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<AttachmentMetadata>> getAssignmentAttachments(
            @PathVariable Long id) {
        return ResponseEntity.ok(service.getAssignmentAttachmentMetadata(id));
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('FACULTY', 'HOD')")
    public ResponseEntity<Assignment> create(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String dueDate,
            @RequestParam(required = false, defaultValue = "100") Integer maxMarks,
            @RequestParam Long subjectId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            Authentication authentication) {

        User currentUser = getAuthenticatedUser(authentication);
        Long creatorId = facultyId != null ? facultyId : currentUser.getId();

        return ResponseEntity.ok(
                service.createWithAttachments(title, description, LocalDate.parse(dueDate), maxMarks, subjectId, creatorId, currentUser, files)
        );
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('FACULTY', 'HOD')")
    public ResponseEntity<Assignment> update(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String dueDate,
            @RequestParam(required = false, defaultValue = "100") Integer maxMarks,
            @RequestParam Long subjectId,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(service.update(
                id, title, description, LocalDate.parse(dueDate), maxMarks, subjectId, currentUser, files));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACULTY', 'HOD', 'ADMIN')")
    public ResponseEntity<String> delete(
            @PathVariable Long id,
            Authentication authentication) {

        User currentUser = getAuthenticatedUser(authentication);
        service.delete(id, currentUser);

        return ResponseEntity.ok("Assignment deleted successfully");
    }

    // ──────── Submissions & Evaluation Endpoints ────────

    @PostMapping(value = "/{id}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AssignmentSubmission> submit(
            @PathVariable Long id,
            @RequestParam(required = false) String submissionText,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            Authentication authentication) {

        User currentUser = getAuthenticatedUser(authentication);

        return ResponseEntity.ok(
                service.submitAssignment(id, currentUser.getId(), submissionText, files)
        );
    }

    @GetMapping("/{id}/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadAssignmentAttachment(
            @PathVariable Long id,
            @PathVariable Long attachmentId) {
        AttachmentFile attachment = service.getAssignmentAttachment(id, attachmentId);
        return attachmentResponse(attachment.fileName(), attachment.contentType(), attachment.data());
    }

    @GetMapping("/submissions/{submissionId}/attachments")
    public ResponseEntity<List<AttachmentMetadata>> getSubmissionAttachments(
            @PathVariable Long submissionId,
            Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(service.getSubmissionAttachmentMetadata(submissionId, currentUser));
    }

    @GetMapping("/submissions/{submissionId}/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> downloadSubmissionAttachment(
            @PathVariable Long submissionId,
            @PathVariable Long attachmentId,
            Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        AttachmentFile attachment =
                service.getSubmissionAttachment(submissionId, attachmentId, currentUser);
        return attachmentResponse(attachment.fileName(), attachment.contentType(), attachment.data());
    }

    private ResponseEntity<Resource> attachmentResponse(String fileName, String contentType, byte[] data) {
        String safeName = (fileName == null || fileName.isBlank())
                ? "download"
                : fileName.replace("\"", "").replace("\r", "").replace("\n", "");
        MediaType type;
        try {
            type = contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
        } catch (Exception exception) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeName + "\"")
                .body(new ByteArrayResource(data));
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
    @PreAuthorize("hasAnyRole('FACULTY', 'HOD', 'ADMIN')")
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
