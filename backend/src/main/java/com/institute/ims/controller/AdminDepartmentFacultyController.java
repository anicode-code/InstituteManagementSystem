package com.institute.ims.controller;

import com.institute.ims.dto.AdminFacultyProfile;
import com.institute.ims.dto.AdminFacultyRosterPage;
import com.institute.ims.model.VerificationStatus;
import com.institute.ims.service.AdminDepartmentFacultyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/departments/{departmentId}/faculty")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDepartmentFacultyController {

    private final AdminDepartmentFacultyService service;

    public AdminDepartmentFacultyController(AdminDepartmentFacultyService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<AdminFacultyRosterPage> getFaculty(
            @PathVariable Long departmentId,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) VerificationStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "facultyId") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(service.getRoster(
                departmentId, academicYear, subjectId, status, search, sort, sortBy, direction, page, size));
    }

    @GetMapping("/{facultyId}")
    public ResponseEntity<AdminFacultyProfile> getProfile(
            @PathVariable Long departmentId,
            @PathVariable Long facultyId) {
        return ResponseEntity.ok(service.getProfile(departmentId, facultyId));
    }
}
