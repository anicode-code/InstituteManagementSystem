package com.institute.ims.controller;

import com.institute.ims.dto.HodFacultyRosterItem;
import com.institute.ims.service.HodFacultyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hod/faculty")
@PreAuthorize("hasRole('HOD')")
public class HodFacultyController {

    private final HodFacultyService service;

    public HodFacultyController(HodFacultyService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<HodFacultyRosterItem>> getRoster(Authentication authentication) {
        return ResponseEntity.ok(service.getRoster(authentication.getName()));
    }
}
