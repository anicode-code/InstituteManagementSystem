package com.institute.ims.service;

import com.institute.ims.dto.AttachmentFile;
import com.institute.ims.dto.StudyMaterialResponse;
import com.institute.ims.model.*;
import com.institute.ims.repository.StudyMaterialRepository;
import com.institute.ims.repository.SubjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@Service
public class StudyMaterialService {

    private final StudyMaterialRepository studyMaterialRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectService subjectService;

    public StudyMaterialService(
            StudyMaterialRepository studyMaterialRepository,
            SubjectRepository subjectRepository,
            SubjectService subjectService) {
        this.studyMaterialRepository = studyMaterialRepository;
        this.subjectRepository = subjectRepository;
        this.subjectService = subjectService;
    }

    @Transactional(readOnly = true)
    public List<StudyMaterialResponse> getMaterials(User requestingUser) {
        List<Long> subjectIds = getAccessibleSubjectIds(requestingUser);
        if (subjectIds.isEmpty()) {
            return List.of();
        }
        return studyMaterialRepository.findMaterialResponsesBySubjectIds(subjectIds);
    }

    @Transactional
    public List<StudyMaterialResponse> upload(
            Long subjectId,
            String title,
            String description,
            MultipartFile[] files,
            User requestingUser) {
        if (requestingUser.getRole() != Role.FACULTY && requestingUser.getRole() != Role.HOD) {
            throw new AccessDeniedException("Only Faculty and HOD users may upload study materials.");
        }
        if (files == null || java.util.Arrays.stream(files).noneMatch(file -> file != null && !file.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose at least one file to upload.");
        }

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        if (!isAssignedToSubject(subject, requestingUser)) {
            throw new AccessDeniedException("You may only upload materials to subjects assigned to you.");
        }

        java.util.ArrayList<StudyMaterialResponse> uploaded = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            StudyMaterial material = new StudyMaterial();
            material.setSubject(subject);
            material.setUploadedBy(requestingUser);
            material.setTitle(title == null || title.isBlank() ? null : title.trim());
            material.setDescription(description == null || description.isBlank() ? null : description.trim());
            material.setFileName(safeFileName(file.getOriginalFilename()));
            material.setContentType(file.getContentType());
            material.setFileSize(file.getSize());
            try {
                material.setData(file.getBytes());
            } catch (IOException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded file.", exception);
            }

            StudyMaterial saved = studyMaterialRepository.save(material);
            uploaded.add(toResponse(saved));
        }
        return uploaded;
    }

    @Transactional(readOnly = true)
    public AttachmentFile download(Long materialId, User requestingUser) {
        StudyMaterial material = studyMaterialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Study material not found"));
        if (!getAccessibleSubjectIds(requestingUser).contains(material.getSubject().getId())) {
            throw new AccessDeniedException("You may only download materials for your subjects.");
        }
        return new AttachmentFile(material.getFileName(), material.getContentType(), material.getData());
    }

    @Transactional
    public void delete(Long materialId, User requestingUser) {
        if (requestingUser.getRole() != Role.FACULTY && requestingUser.getRole() != Role.HOD) {
            throw new AccessDeniedException("Only Faculty and HOD users may delete study materials.");
        }

        StudyMaterial material = studyMaterialRepository.findById(materialId)
                .orElseThrow(() -> new RuntimeException("Study material not found"));
        if (!material.getUploadedBy().getId().equals(requestingUser.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only delete study materials you uploaded.");
        }

        studyMaterialRepository.delete(material);
    }

    private List<Long> getAccessibleSubjectIds(User user) {
        return switch (user.getRole()) {
            case STUDENT -> subjectService.getByStudent(user.getId()).stream()
                    .map(Subject::getId)
                    .toList();
            case FACULTY, HOD -> subjectRepository.findByFacultyId(user.getId()).stream()
                    .map(Subject::getId)
                    .toList();
            default -> throw new AccessDeniedException("You may not access study materials.");
        };
    }

    private boolean isAssignedToSubject(Subject subject, User user) {
        return subject.getFacultyMembers().stream()
                .anyMatch(member -> member.getId().equals(user.getId()));
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "study-material";
        }
        String normalized = fileName.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private StudyMaterialResponse toResponse(StudyMaterial material) {
        Subject subject = material.getSubject();
        return new StudyMaterialResponse(
                material.getId(),
                material.getTitle(),
                material.getDescription(),
                material.getFileName(),
                material.getContentType(),
                material.getFileSize(),
                subject.getId(),
                subject.getName(),
                subject.getCode(),
                material.getUploadedBy().getId(),
                material.getUploadedBy().getName(),
                material.getUploadedAt());
    }
}
