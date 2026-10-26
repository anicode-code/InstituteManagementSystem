package com.institute.ims.service;

import com.institute.ims.common.ConflictException;
import com.institute.ims.common.ResourceNotFoundException;
import com.institute.ims.dto.DepartmentSummaryResponse;
import com.institute.ims.dto.SubjectFacultyRequestResponse;
import com.institute.ims.dto.SubjectSummaryResponse;
import com.institute.ims.model.*;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.FacultySubjectRequestRepository;
import com.institute.ims.repository.SubjectRepository;
import com.institute.ims.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FacultySubjectRequestService {

    private final FacultySubjectRequestRepository requestRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public FacultySubjectRequestService(
            FacultySubjectRequestRepository requestRepository,
            SubjectRepository subjectRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository) {
        this.requestRepository = requestRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<SubjectSummaryResponse> getEligibleSubjects(User faculty) {
        faculty = loadUser(faculty);
        requireVerifiedFaculty(faculty);
        Set<Long> departmentIds = getFacultyDepartmentIds(faculty);
        if (departmentIds.isEmpty()) {
            return List.of();
        }
        return subjectRepository.findByDepartmentIdIn(List.copyOf(departmentIds)).stream()
                .map(SubjectSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubjectFacultyRequestResponse> getMyRequests(User faculty) {
        faculty = loadUser(faculty);
        requireVerifiedFaculty(faculty);
        return requestRepository.findByFacultyIdOrderByCreatedAtDesc(faculty.getId()).stream()
                .map(SubjectFacultyRequestResponse::from)
                .toList();
    }

    @Transactional
    public SubjectFacultyRequestResponse submitRequest(User faculty, Long subjectId) {
        User requestingFaculty = loadUser(faculty);
        requireVerifiedFaculty(requestingFaculty);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found."));
        requireFacultyDepartmentEligibility(requestingFaculty, subject);

        if (subject.getFacultyMembers().stream().anyMatch(member -> member.getId().equals(requestingFaculty.getId()))) {
            throw new ConflictException("You are already assigned to this subject.");
        }
        if (requestRepository.findFirstByFacultyIdAndSubjectIdAndStatusOrderByCreatedAtDesc(
                requestingFaculty.getId(), subjectId, FacultySubjectRequestStatus.PENDING).isPresent()) {
            throw new ConflictException("A pending request for this subject already exists.");
        }

        FacultySubjectRequest request = new FacultySubjectRequest();
        request.setFaculty(requestingFaculty);
        request.setSubject(subject);
        request.setStatus(FacultySubjectRequestStatus.PENDING);
        return SubjectFacultyRequestResponse.from(requestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<DepartmentSummaryResponse> getHodDepartments(User hod) {
        hod = loadUser(hod);
        requireHod(hod);
        return departmentRepository.findByHodId(hod.getId()).stream()
                .map(DepartmentSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubjectSummaryResponse> getHodSubjects(User hod) {
        hod = loadUser(hod);
        Set<Long> departmentIds = getHodDepartmentIds(hod);
        if (departmentIds.isEmpty()) {
            return List.of();
        }
        return subjectRepository.findByDepartmentIdIn(List.copyOf(departmentIds)).stream()
                .map(SubjectSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubjectFacultyRequestResponse> getHodRequests(
            User hod, Long departmentId, FacultySubjectRequestStatus status) {
        hod = loadUser(hod);
        Set<Long> departmentIds = getHodDepartmentIds(hod);
        if (departmentId != null && !departmentIds.contains(departmentId)) {
            throw new AccessDeniedException("You may only manage requests in your appointed departments.");
        }
        return requestRepository.findByOrderByCreatedAtDesc().stream()
                .filter(request -> {
                    var requestDepartment = request.getSubject().getDepartment();
                    Long requestDepartmentId = requestDepartment == null ? null : requestDepartment.getId();
                    return requestDepartmentId != null && departmentIds.contains(requestDepartmentId)
                            && (departmentId == null || departmentId.equals(requestDepartmentId))
                            && (status == null || request.getStatus() == status);
                })
                .map(SubjectFacultyRequestResponse::from)
                .toList();
    }

    @Transactional
    public SubjectFacultyRequestResponse decide(
            User hod, Long requestId, FacultySubjectRequestStatus decision) {
        hod = loadUser(hod);
        requireHod(hod);
        if (decision != FacultySubjectRequestStatus.APPROVED
                && decision != FacultySubjectRequestStatus.REJECTED) {
            throw new IllegalArgumentException("Decision must be APPROVED or REJECTED.");
        }

        FacultySubjectRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject request not found."));
        Subject subject = request.getSubject();
        if (subject.getDepartment() == null
                || !isHodAuthorizedForDepartment(hod, subject.getDepartment().getId())) {
            throw new AccessDeniedException("You may only decide requests in your appointed departments.");
        }
        if (request.getStatus() != FacultySubjectRequestStatus.PENDING) {
            throw new ConflictException("This request has already been decided.");
        }

        if (decision == FacultySubjectRequestStatus.APPROVED) {
            User faculty = request.getFaculty();
            requireVerifiedFaculty(faculty);
            requireFacultyDepartmentEligibility(faculty, subject);
            boolean alreadyAssigned = subject.getFacultyMembers().stream()
                    .anyMatch(member -> member.getId().equals(faculty.getId()));
            if (!alreadyAssigned) {
                subject.getFacultyMembers().add(faculty);
                subjectRepository.save(subject);
            }
        }

        request.setStatus(decision);
        request.setReviewedBy(hod);
        request.setReviewedAt(LocalDateTime.now());
        return SubjectFacultyRequestResponse.from(requestRepository.save(request));
    }

    Set<Long> getFacultyDepartmentIds(User faculty) {
        Set<Long> departmentIds = faculty.getDepartments().stream()
                .map(Department::getId)
                .collect(Collectors.toSet());
        if (departmentIds.isEmpty() && faculty.getDepartment() != null) {
            departmentIds.add(faculty.getDepartment().getId());
        }
        return departmentIds;
    }

    void requireVerifiedFaculty(User faculty) {
        if ((faculty.getRole() != Role.FACULTY && faculty.getRole() != Role.HOD)
                || faculty.getStatus() != VerificationStatus.VERIFIED) {
            throw new AccessDeniedException("Only verified faculty members may request or teach subjects.");
        }
    }

    void requireFacultyDepartmentEligibility(User faculty, Subject subject) {
        if (subject.getDepartment() == null
                || !getFacultyDepartmentIds(faculty).contains(subject.getDepartment().getId())) {
            throw new AccessDeniedException("You may only request subjects in your registered departments.");
        }
    }

    Set<Long> getHodDepartmentIds(User hod) {
        requireHod(hod);
        return departmentRepository.findByHodId(hod.getId()).stream()
                .map(Department::getId)
                .collect(Collectors.toSet());
    }

    void requireHod(User hod) {
        if (hod.getRole() != Role.HOD) {
            throw new AccessDeniedException("Only HODs may manage subject requests.");
        }
    }

    boolean isHodAuthorizedForDepartment(User hod, Long departmentId) {
        return getHodDepartmentIds(hod).contains(departmentId);
    }

    private User loadUser(User user) {
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}
