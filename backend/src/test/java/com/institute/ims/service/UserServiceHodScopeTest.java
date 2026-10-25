package com.institute.ims.service;

import com.institute.ims.model.Department;
import com.institute.ims.model.Role;
import com.institute.ims.model.User;
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
import static org.mockito.Mockito.when;

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
