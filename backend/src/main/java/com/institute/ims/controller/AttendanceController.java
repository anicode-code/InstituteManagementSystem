package com.institute.ims.controller;

import com.institute.ims.model.Attendance;
import com.institute.ims.model.AttendanceStatus;
import com.institute.ims.model.Role;
import com.institute.ims.model.Subject;
import com.institute.ims.model.User;
import com.institute.ims.service.AttendanceService;
import com.institute.ims.service.SubjectService;
import com.institute.ims.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService service;
    private final UserService userService;
    private final SubjectService subjectService;

    public AttendanceController(
            AttendanceService service,
            UserService userService,
            SubjectService subjectService) {

        this.service = service;
        this.userService = userService;
        this.subjectService = subjectService;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userService.getUserByRollNumber(authentication.getName());
    }

    @GetMapping
    public ResponseEntity<List<Attendance>> getAll(Authentication authentication) {
        User currentUser = getAuthenticatedUser(authentication);
        return ResponseEntity.ok(service.getScopedAttendance(currentUser));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Attendance>> getByStudent(
            @PathVariable Long studentId,
            Authentication authentication) {

        User currentUser = getAuthenticatedUser(authentication);
        if (currentUser.getRole() == Role.STUDENT && !currentUser.getId().equals(studentId)) {
            throw new RuntimeException("Unauthorized: Students may only view their own attendance records.");
        }

        return ResponseEntity.ok(service.getByStudent(studentId));
    }

    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<List<Attendance>> getBySubject(
            @PathVariable Long subjectId,
            Authentication authentication) {

        if (authentication != null) {
            User currentUser = getAuthenticatedUser(authentication);
            if (currentUser != null && currentUser.getRole() == Role.STUDENT) {
                // Ensure student is enrolled in this subject
                List<Subject> enrolled = subjectService.getByStudent(currentUser.getId());
                boolean isEnrolled = enrolled.stream().anyMatch(s -> s.getId().equals(subjectId));
                if (!isEnrolled) {
                    throw new RuntimeException("Unauthorized: You are not enrolled in this subject.");
                }
                // Return only this student's attendance in this subject
                List<Attendance> studentAttendance = service.getByStudent(currentUser.getId()).stream()
                        .filter(a -> a.getSubject() != null && a.getSubject().getId().equals(subjectId))
                        .toList();
                return ResponseEntity.ok(studentAttendance);
            }
        }

        return ResponseEntity.ok(service.getBySubject(subjectId));
    }

    @GetMapping("/percentage")
    public ResponseEntity<Double> getPercentage(
            @RequestParam Long studentId,
            @RequestParam(required = false) Long subjectId,
            Authentication authentication) {

        User currentUser = getAuthenticatedUser(authentication);
        if (currentUser.getRole() == Role.STUDENT && !currentUser.getId().equals(studentId)) {
            throw new RuntimeException("Unauthorized: Students may only view their own attendance percentage.");
        }

        return ResponseEntity.ok(
                service.getAttendancePercentage(studentId, subjectId)
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN', 'HOD')")
    public ResponseEntity<Attendance> create(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        User currentUser = getAuthenticatedUser(authentication);

        Long studentId = Long.parseLong(body.get("studentId").toString());
        Long subjectId = Long.parseLong(body.get("subjectId").toString());
        LocalDate date = LocalDate.parse(body.get("date").toString());
        AttendanceStatus status = AttendanceStatus.valueOf(body.get("status").toString());

        Integer performanceMarks = null;
        if (body.get("performanceMarks") != null && !body.get("performanceMarks").toString().trim().isEmpty()) {
            performanceMarks = Integer.parseInt(body.get("performanceMarks").toString().trim());
        }

        return ResponseEntity.ok(
                service.createOrUpdate(studentId, subjectId, date, status, performanceMarks, currentUser)
        );
    }

    @PostMapping("/batch-day")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN', 'HOD')")
    public ResponseEntity<List<Attendance>> recordDayBatch(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        User currentUser = getAuthenticatedUser(authentication);

        Long subjectId = Long.parseLong(body.get("subjectId").toString());
        LocalDate date = LocalDate.parse(body.get("date").toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) body.get("entries");

        return ResponseEntity.ok(
                service.recordDayBatch(subjectId, date, entries, currentUser)
        );
    }

    @DeleteMapping("/day")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN', 'HOD')")
    public ResponseEntity<String> deleteDay(
            @RequestParam Long subjectId,
            @RequestParam String date,
            Authentication authentication) {

        User currentUser = getAuthenticatedUser(authentication);
        service.deleteDay(subjectId, LocalDate.parse(date), currentUser);
        return ResponseEntity.ok("Day attendance session deleted successfully.");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN', 'HOD')")
    public ResponseEntity<Attendance> update(
            @PathVariable Long id,
            @RequestParam(required = false) AttendanceStatus status,
            @RequestBody(required = false) Map<String, Object> body,
            Authentication authentication) {

        User currentUser = getAuthenticatedUser(authentication);

        AttendanceStatus effectiveStatus = status;
        Integer performanceMarks = null;

        if (body != null) {
            if (body.get("status") != null) {
                effectiveStatus = AttendanceStatus.valueOf(body.get("status").toString());
            }
            if (body.get("performanceMarks") != null && !body.get("performanceMarks").toString().trim().isEmpty()) {
                performanceMarks = Integer.parseInt(body.get("performanceMarks").toString().trim());
            }
        }

        if (effectiveStatus == null) {
            throw new RuntimeException("Status is required");
        }

        return ResponseEntity.ok(service.update(id, effectiveStatus, performanceMarks, currentUser));
    }
}
