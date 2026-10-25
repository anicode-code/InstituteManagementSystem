package com.institute.ims.controller;

import com.institute.ims.dto.HodStudentDetails;
import com.institute.ims.dto.HodStudentRosterItem;
import com.institute.ims.service.HodStudentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hod/students")
@PreAuthorize("hasRole('HOD')")
public class HodStudentController {

    private final HodStudentService service;

    public HodStudentController(HodStudentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<HodStudentRosterItem>> getRoster(
            @RequestParam Integer year,
            @RequestParam(defaultValue = "rollNumber") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            Authentication authentication) {
        return ResponseEntity.ok(service.getRoster(
                authentication.getName(), year, sortBy, direction));
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<HodStudentDetails> getStudentDetails(
            @PathVariable Long studentId,
            Authentication authentication) {
        return ResponseEntity.ok(service.getStudentDetails(
                authentication.getName(), studentId));
    }
}
