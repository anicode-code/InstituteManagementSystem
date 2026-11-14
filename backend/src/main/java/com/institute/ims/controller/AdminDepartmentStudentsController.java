package com.institute.ims.controller;

import com.institute.ims.dto.AdminStudentProfile;
import com.institute.ims.dto.AdminStudentRosterPage;
import com.institute.ims.model.VerificationStatus;
import com.institute.ims.service.AdminDepartmentStudentsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/departments/{departmentId}/students")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDepartmentStudentsController {

    private final AdminDepartmentStudentsService service;

    public AdminDepartmentStudentsController(AdminDepartmentStudentsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<AdminStudentRosterPage> getStudents(
            @PathVariable Long departmentId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) VerificationStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String attention,
            @RequestParam(defaultValue = "rollNumber") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(service.getRoster(
                departmentId, year, semester, status, search, attention, sortBy, direction, page, size));
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<AdminStudentProfile> getStudentProfile(
            @PathVariable Long departmentId,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(service.getStudentProfile(departmentId, studentId));
    }
}
