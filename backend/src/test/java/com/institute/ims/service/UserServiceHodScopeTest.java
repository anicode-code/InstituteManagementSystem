package com.institute.ims.service;

import com.institute.ims.dto.VerifyBulkRequest;
import com.institute.ims.model.Department;
import com.institute.ims.model.Role;
import com.institute.ims.model.User;
import com.institute.ims.model.VerificationStatus;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceHodScopeTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService service;

    @Test
    void hodUserListsAndProfilesAreLimitedToAssignedDepartmentStudents() {
        User hod = user(1L, "HOD", Role.HOD, null);
        Department ownDepartment = department(10L);
        User localStudent = user(2L, "LOCAL", Role.STUDENT, ownDepartment);
        User foreignStudent = user(3L, "FOREIGN", Role.STUDENT, department(11L));
        when(userRepository.findByRollNumber("HOD")).thenReturn(Optional.of(hod));
        when(userRepository.findByRole(Role.STUDENT)).thenReturn(List.of(localStudent, foreignStudent));
        when(userRepository.findById(3L)).thenReturn(Optional.of(foreignStudent));
        when(departmentRepository.findByHodId(1L)).thenReturn(List.of(ownDepartment));

        assertThat(service.getUsersForRequestingUser(Role.STUDENT, "HOD"))
                .containsExactly(localStudent);
        assertThatThrownBy(() -> service.getUserById(3L, "HOD"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("assigned department");
    }

    @Test
    void hodCanRejectPendingStudentsOnlyInAssignedDepartments() {
        User hod = user(1L, "HOD", Role.HOD, null);
        Department ownDepartment = department(10L);
        User localStudent = user(2L, "LOCAL", Role.STUDENT, ownDepartment);
        localStudent.setStatus(VerificationStatus.PENDING);
        User foreignStudent = user(3L, "FOREIGN", Role.STUDENT, department(11L));
        foreignStudent.setStatus(VerificationStatus.PENDING);
        when(userRepository.findByRollNumber("HOD")).thenReturn(Optional.of(hod));
        when(userRepository.findById(2L)).thenReturn(Optional.of(localStudent));
        when(userRepository.findById(3L)).thenReturn(Optional.of(foreignStudent));
        when(departmentRepository.findByHodId(1L)).thenReturn(List.of(ownDepartment));

        service.rejectUser(2L, "HOD");

        verify(userRepository).delete(localStudent);
        assertThatThrownBy(() -> service.rejectUser(3L, "HOD"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("assigned department");
    }

    @Test
    void rejectionCannotDeleteAnAlreadyVerifiedUser() {
        User admin = user(1L, "ADMIN", Role.ADMIN, null);
        User verifiedStudent = user(2L, "STUDENT", Role.STUDENT, department(10L));
        verifiedStudent.setStatus(VerificationStatus.VERIFIED);
        when(userRepository.findByRollNumber("ADMIN")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(verifiedStudent));

        assertThatThrownBy(() -> service.rejectUser(2L, "ADMIN"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only pending registrations");

        verify(userRepository, never()).delete(verifiedStudent);
    }

    @Test
    void bulkRejectionDeletesAllSelectedPendingUsers() {
        User admin = user(1L, "ADMIN", Role.ADMIN, null);
        User firstStudent = user(2L, "FIRST", Role.STUDENT, department(10L));
        firstStudent.setStatus(VerificationStatus.PENDING);
        User secondStudent = user(3L, "SECOND", Role.STUDENT, department(10L));
        secondStudent.setStatus(VerificationStatus.PENDING);
        when(userRepository.findByRollNumber("ADMIN")).thenReturn(Optional.of(admin));
        when(userRepository.findByIdIn(List.of(2L, 3L))).thenReturn(List.of(firstStudent, secondStudent));

        int rejectedCount = service.rejectUsers(
                new VerifyBulkRequest(List.of(2L, 3L)), "ADMIN");

        assertThat(rejectedCount).isEqualTo(2);
        verify(userRepository).deleteAll(List.of(firstStudent, secondStudent));
    }

    @Test
    void bulkRejectionValidatesAllUsersBeforeDeletingAny() {
        User admin = user(1L, "ADMIN", Role.ADMIN, null);
        User pendingStudent = user(2L, "PENDING", Role.STUDENT, department(10L));
        pendingStudent.setStatus(VerificationStatus.PENDING);
        User verifiedStudent = user(3L, "VERIFIED", Role.STUDENT, department(10L));
        verifiedStudent.setStatus(VerificationStatus.VERIFIED);
        when(userRepository.findByRollNumber("ADMIN")).thenReturn(Optional.of(admin));
        when(userRepository.findByIdIn(List.of(2L, 3L))).thenReturn(List.of(pendingStudent, verifiedStudent));

        assertThatThrownBy(() -> service.rejectUsers(
                new VerifyBulkRequest(List.of(2L, 3L)), "ADMIN"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only pending registrations");

        verify(userRepository, never()).deleteAll(List.of(pendingStudent, verifiedStudent));
    }

    private User user(Long id, String rollNumber, Role role, Department department) {
        User user = new User();
        user.setId(id);
        user.setRollNumber(rollNumber);
        user.setRole(role);
        user.setDepartment(department);
        return user;
    }

    private Department department(Long id) {
        Department department = new Department();
        department.setId(id);
        return department;
    }
}
