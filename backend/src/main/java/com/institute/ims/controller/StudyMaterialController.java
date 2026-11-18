package com.institute.ims.controller;

import com.institute.ims.dto.AttachmentFile;
import com.institute.ims.dto.StudyMaterialResponse;
import com.institute.ims.model.User;
import com.institute.ims.service.StudyMaterialService;
import com.institute.ims.service.UserService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/study-materials")
public class StudyMaterialController {

    private final StudyMaterialService studyMaterialService;
    private final UserService userService;

    public StudyMaterialController(StudyMaterialService studyMaterialService, UserService userService) {
        this.studyMaterialService = studyMaterialService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HOD', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<StudyMaterialResponse>> getMaterials(Authentication authentication) {
        User user = userService.getUserByRollNumber(authentication.getName());
        return ResponseEntity.ok(studyMaterialService.getMaterials(user));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('HOD', 'FACULTY')")
    public ResponseEntity<List<StudyMaterialResponse>> upload(
            @RequestParam Long subjectId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            Authentication authentication) {
        User user = userService.getUserByRollNumber(authentication.getName());
        return ResponseEntity.ok(studyMaterialService.upload(subjectId, title, description, files, user));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('HOD', 'FACULTY', 'STUDENT')")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication authentication) {
        User user = userService.getUserByRollNumber(authentication.getName());
        AttachmentFile file = studyMaterialService.download(id, user);
        String fileName = file.fileName() == null || file.fileName().isBlank()
                ? "study-material"
                : file.fileName().replace("\"", "").replace("\r", "").replace("\n", "");
        MediaType contentType;
        try {
            contentType = file.contentType() == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(file.contentType());
        } catch (IllegalArgumentException exception) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(new ByteArrayResource(file.data()));
    }
}
