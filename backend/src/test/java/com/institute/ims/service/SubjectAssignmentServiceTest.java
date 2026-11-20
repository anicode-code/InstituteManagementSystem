package com.institute.ims.service;

import com.institute.ims.common.ConflictException;
import com.institute.ims.model.*;
import com.institute.ims.repository.SubjectRepository;
import com.institute.ims.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class SubjectAssignmentServiceTest {

    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FacultySubjectRequestService eligibilityService;

    private SubjectAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new SubjectAssignmentService(subjectRepository, userRepository, eligibilityService);
    }

    @Test
    void hodCanAssignThemselvesAndDepartmentIsAssociated() {
        User hod = user(10L, Role.HOD);
        Department department = department(20L);
        Subject subject = subject(30L, department);
        subject.setFacultyMembers(new HashSet<>());
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject));
        when(userRepository.findById(10L)).thenReturn(Optional.of(hod));

        var result = service.assign(hod, 30L, 10L);

        assertThat(result).hasSize(1);
        assertThat(subject.getFacultyMembers()).contains(hod);
        verify(eligibilityService).requireVerifiedFaculty(hod);
        verify(eligibilityService).associateFacultyWithDepartment(hod, department);
        verify(subjectRepository).save(subject);
    }

    @Test
    void hodCanAssignThemselvesToAnyDepartmentSubject() {
        User hod = user(10L, Role.HOD);
        Department otherDepartment = department(21L);
        Subject subject = subject(30L, otherDepartment);
        subject.setFacultyMembers(new HashSet<>());
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject));
        when(userRepository.findById(10L)).thenReturn(Optional.of(hod));

        var result = service.assign(hod, 30L, 10L);

        assertThat(result).extracting("id").containsExactly(10L);
        verify(eligibilityService).associateFacultyWithDepartment(hod, otherDepartment);
        verify(subjectRepository).save(subject);
    }

    @Test
    void hodCanAssignRegularFacultyToTheirDepartment() {
        User hod = user(10L, Role.HOD);
        User faculty = user(11L, Role.FACULTY);
        Department department = department(20L);
        Subject subject = subject(30L, department);
        subject.setFacultyMembers(new HashSet<>());
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject));
        when(eligibilityService.isHodAuthorizedForDepartment(hod, 20L)).thenReturn(true);
        when(userRepository.findById(11L)).thenReturn(Optional.of(faculty));

        var result = service.assign(hod, 30L, 11L);

        assertThat(result).extracting("id").containsExactly(11L);
        verify(eligibilityService).associateFacultyWithDepartment(faculty, department);
        verify(subjectRepository).save(subject);
    }

    @Test
    void hodCannotAssignAnotherHod() {
        User hod = user(10L, Role.HOD);
        User otherHod = user(11L, Role.HOD);
        Subject subject = subject(30L, department(20L));
        subject.setFacultyMembers(new HashSet<>());
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject));
        when(eligibilityService.isHodAuthorizedForDepartment(hod, 20L)).thenReturn(true);
        when(userRepository.findById(11L)).thenReturn(Optional.of(otherHod));

        assertThatThrownBy(() -> service.assign(hod, 30L, 11L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("regular faculty members");

        verify(eligibilityService, never()).associateFacultyWithDepartment(any(), any());
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void eligibleFacultyForOtherDepartmentSubjectContainsOnlyTheHod() {
        User hod = user(10L, Role.HOD);
        Subject subject = subject(30L, department(21L));
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject));
        when(eligibilityService.isHodAuthorizedForDepartment(hod, 21L)).thenReturn(false);

        var result = service.getEligibleFaculty(hod, 30L);

        assertThat(result).extracting("id").containsExactly(10L);
        verify(userRepository, never()).findByRoleAndStatus(any(), any());
    }

    @Test
    void eligibleFacultyForOwnDepartmentIncludesRegularFacultyAndSelf() {
        User hod = user(10L, Role.HOD);
        User faculty = user(11L, Role.FACULTY);
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject(30L, department(20L))));
        when(eligibilityService.isHodAuthorizedForDepartment(hod, 20L)).thenReturn(true);
        when(userRepository.findByRoleAndStatus(Role.FACULTY, VerificationStatus.VERIFIED))
                .thenReturn(List.of(faculty));

        var result = service.getEligibleFaculty(hod, 30L);

        assertThat(result).extracting("id").containsExactly(11L, 10L);
    }

    @Test
    void duplicateActiveAssignmentReturnsConflict() {
        User hod = user(10L, Role.HOD);
        Department department = department(20L);
        Subject subject = subject(30L, department);
        User faculty = user(11L, Role.FACULTY);
        subject.setFacultyMembers(new HashSet<>(Set.of(faculty)));
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject));
        when(eligibilityService.isHodAuthorizedForDepartment(hod, 20L)).thenReturn(true);
        when(userRepository.findById(11L)).thenReturn(Optional.of(faculty));

        assertThatThrownBy(() -> service.assign(hod, 30L, 11L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already assigned");

        verify(subjectRepository, never()).save(any());
    }

    @Test
    void hodCannotAssignFacultyOutsideTheirDepartmentScope() {
        User hod = user(10L, Role.HOD);
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject(30L, department(20L))));
        when(eligibilityService.isHodAuthorizedForDepartment(hod, 20L)).thenReturn(false);

        assertThatThrownBy(() -> service.assign(hod, 30L, 11L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("appointed departments");

        verify(userRepository, never()).findById(11L);
        verify(subjectRepository, never()).save(any());
    }

    @Test
    void removingFacultyLeavesOtherTeachersOnSubject() {
        User hod = user(10L, Role.HOD);
        User removedFaculty = user(11L, Role.FACULTY);
        User remainingFaculty = user(12L, Role.FACULTY);
        Subject subject = subject(30L, department(20L));
        subject.setFacultyMembers(new HashSet<>(Set.of(removedFaculty, remainingFaculty)));
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject));
        when(eligibilityService.isHodAuthorizedForDepartment(hod, 20L)).thenReturn(true);

        var result = service.remove(hod, 30L, 11L);

        assertThat(result).extracting("id").containsExactly(12L);
        assertThat(subject.getFacultyMembers()).containsExactly(remainingFaculty);
        verify(subjectRepository).save(subject);
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus(VerificationStatus.VERIFIED);
        user.setName("User " + id);
        user.setRollNumber("U" + id);
        return user;
    }

    private Department department(Long id) {
        Department department = new Department();
        department.setId(id);
        return department;
    }

    private Subject subject(Long id, Department department) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setDepartment(department);
        return subject;
    }
}
