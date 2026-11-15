package com.institute.ims.controller;

import com.institute.ims.dto.AdminHodProfile;
import com.institute.ims.dto.AdminHodDirectory;
import com.institute.ims.service.AdminDepartmentHodService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/departments/{departmentId}/hod")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDepartmentHodController {

    private final AdminDepartmentHodService service;

    public AdminDepartmentHodController(AdminDepartmentHodService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<AdminHodDirectory> getCurrentHod(
            @PathVariable Long departmentId) {
        return ResponseEntity.ok(service.getDirectory(departmentId));
    }

    @GetMapping("/{hodId}")
    public ResponseEntity<AdminHodProfile> getHodProfile(
            @PathVariable Long departmentId,
            @PathVariable Long hodId) {
        return ResponseEntity.ok(service.getProfile(departmentId, hodId));
    }
}
