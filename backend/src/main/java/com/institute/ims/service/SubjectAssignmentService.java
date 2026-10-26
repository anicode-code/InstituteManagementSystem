package com.institute.ims.service;

import com.institute.ims.common.ConflictException;
import com.institute.ims.common.ResourceNotFoundException;
import com.institute.ims.dto.FacultySummaryResponse;
import com.institute.ims.model.*;
import com.institute.ims.repository.SubjectRepository;
import com.institute.ims.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubjectAssignmentService {

    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final FacultySubjectRequestService eligibilityService;

    public SubjectAssignmentService(
            SubjectRepository subjectRepository,
            UserRepository userRepository,
            FacultySubjectRequestService eligibilityService) {
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.eligibilityService = eligibilityService;
    }

    @Transactional(readOnly = true)
    public List<FacultySummaryResponse> getAssignedFaculty(User hod, Long subjectId) {
        Subject subject = getScopedSubject(hod, subjectId);
        return subject.getFacultyMembers().stream().map(FacultySummaryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<FacultySummaryResponse> getEligibleFaculty(User hod, Long subjectId) {
        Subject subject = getScopedSubject(hod, subjectId);
        Long departmentId = subject.getDepartment().getId();
        return userRepository.findByDepartmentsContainingAndRoleInAndStatus(
                        departmentId,
                        List.of(Role.FACULTY, Role.HOD),
                        VerificationStatus.VERIFIED)
                .stream()
                .map(FacultySummaryResponse::from)
                .toList();
    }

    @Transactional
    public List<FacultySummaryResponse> assign(User hod, Long subjectId, Long facultyId) {
        Subject subject = getScopedSubject(hod, subjectId);
        User faculty = userRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty member not found."));
        eligibilityService.requireVerifiedFaculty(faculty);
        eligibilityService.requireFacultyDepartmentEligibility(faculty, subject);
        if (subject.getFacultyMembers().stream().anyMatch(member -> member.getId().equals(facultyId))) {
            throw new ConflictException("This faculty member is already assigned to the subject.");
        }
        subject.getFacultyMembers().add(faculty);
        subjectRepository.save(subject);
        return subject.getFacultyMembers().stream().map(FacultySummaryResponse::from).toList();
    }

    @Transactional
    public List<FacultySummaryResponse> remove(User hod, Long subjectId, Long facultyId) {
        Subject subject = getScopedSubject(hod, subjectId);
        boolean removed = subject.getFacultyMembers()
                .removeIf(member -> member.getId().equals(facultyId));
        if (!removed) {
            throw new ConflictException("This faculty member is not assigned to the subject.");
        }
        subjectRepository.save(subject);
        return subject.getFacultyMembers().stream().map(FacultySummaryResponse::from).toList();
    }

    private Subject getScopedSubject(User hod, Long subjectId) {
        if (hod.getRole() != Role.HOD) {
            throw new AccessDeniedException("Only HODs may manage subject assignments.");
        }
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found."));
        if (subject.getDepartment() == null
                || !eligibilityService.isHodAuthorizedForDepartment(hod, subject.getDepartment().getId())) {
            throw new AccessDeniedException("You may only manage subjects in your appointed departments.");
        }
        return subject;
    }
}
