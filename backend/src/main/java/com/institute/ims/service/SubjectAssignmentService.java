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

import java.util.ArrayList;
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
        Subject subject = getSubject(hod, subjectId);
        List<User> eligibleFaculty = new ArrayList<>();
        if (isInHodDepartment(hod, subject)) {
            eligibleFaculty.addAll(userRepository.findByRoleAndStatus(
                    Role.FACULTY, VerificationStatus.VERIFIED));
        }
        if (hod.getStatus() == VerificationStatus.VERIFIED) {
            eligibleFaculty.add(hod);
        }
        return eligibleFaculty.stream().map(FacultySummaryResponse::from).toList();
    }

    @Transactional
    public List<FacultySummaryResponse> assign(User hod, Long subjectId, Long facultyId) {
        Subject subject = getSubject(hod, subjectId);
        boolean assigningSelf = hod.getId().equals(facultyId);
        if (!assigningSelf) {
            requireHodDepartmentScope(hod, subject);
        }
        User faculty = userRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty member not found."));
        if (assigningSelf && faculty.getRole() != Role.HOD) {
            throw new AccessDeniedException("Only an HOD may assign themselves across departments.");
        }
        if (!assigningSelf && faculty.getRole() != Role.FACULTY) {
            throw new AccessDeniedException("HODs may only assign regular faculty members.");
        }
        eligibilityService.requireVerifiedFaculty(faculty);
        if (subject.getFacultyMembers().stream().anyMatch(member -> member.getId().equals(facultyId))) {
            throw new ConflictException("This faculty member is already assigned to the subject.");
        }
        eligibilityService.associateFacultyWithDepartment(faculty, subject.getDepartment());
        subject.getFacultyMembers().add(faculty);
        subjectRepository.save(subject);
        return subject.getFacultyMembers().stream().map(FacultySummaryResponse::from).toList();
    }

    @Transactional
    public List<FacultySummaryResponse> remove(User hod, Long subjectId, Long facultyId) {
        Subject subject = getSubject(hod, subjectId);
        boolean removingSelf = hod.getId().equals(facultyId);
        if (!removingSelf) {
            requireHodDepartmentScope(hod, subject);
            User faculty = subject.getFacultyMembers().stream()
                    .filter(member -> member.getId().equals(facultyId))
                    .findFirst()
                    .orElseThrow(() -> new ConflictException("This faculty member is not assigned to the subject."));
            if (faculty.getRole() != Role.FACULTY) {
                throw new AccessDeniedException("HODs may only remove regular faculty from their departments.");
            }
        }
        boolean removed = subject.getFacultyMembers()
                .removeIf(member -> member.getId().equals(facultyId));
        if (!removed) {
            throw new ConflictException("This faculty member is not assigned to the subject.");
        }
        subjectRepository.save(subject);
        return subject.getFacultyMembers().stream().map(FacultySummaryResponse::from).toList();
    }

    private Subject getScopedSubject(User hod, Long subjectId) {
        Subject subject = getSubject(hod, subjectId);
        requireHodDepartmentScope(hod, subject);
        return subject;
    }

    private Subject getSubject(User hod, Long subjectId) {
        if (hod.getRole() != Role.HOD) {
            throw new AccessDeniedException("Only HODs may manage subject assignments.");
        }
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found."));
        if (subject.getDepartment() == null) {
            throw new ConflictException("This subject is not associated with a department.");
        }
        return subject;
    }

    private boolean isInHodDepartment(User hod, Subject subject) {
        return subject.getDepartment() != null
                && eligibilityService.isHodAuthorizedForDepartment(hod, subject.getDepartment().getId());
    }

    private void requireHodDepartmentScope(User hod, Subject subject) {
        if (!isInHodDepartment(hod, subject)) {
            throw new AccessDeniedException("You may only manage subjects in your appointed departments.");
        }
    }
}
