package com.institute.ims.service;

import com.institute.ims.model.Department;
import com.institute.ims.model.Role;
import com.institute.ims.model.User;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HodFacultyServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private HodFacultyService service;

    private User hod;
    private Department firstDepartment;
    private Department secondDepartment;

    @BeforeEach
    void setUp() {
        hod = new User();
        hod.setId(10L);
        hod.setRollNumber("HOD_TEST");
        hod.setRole(Role.HOD);

        firstDepartment = department(20L, "Computer Science", "CSE");
        secondDepartment = department(21L, "Information Technology", "IT");
    }

    @Test
    void rosterIncludesEachFacultyMemberOnceAcrossAssignedDepartments() {
        User faculty = faculty(1L, "FAC001", "Alex Faculty");
        faculty.setDepartments(Set.of(firstDepartment, secondDepartment));
        when(userRepository.findByRollNumber("HOD_TEST")).thenReturn(java.util.Optional.of(hod));
        when(departmentRepository.findByHodId(10L)).thenReturn(List.of(firstDepartment, secondDepartment));
        when(userRepository.findByRoleAndAssociatedDepartmentId(Role.FACULTY, 20L)).thenReturn(List.of(faculty));
        when(userRepository.findByRoleAndAssociatedDepartmentId(Role.FACULTY, 21L)).thenReturn(List.of(faculty));
        when(userRepository.findByRoleAndAssociatedDepartmentId(Role.HOD, 20L)).thenReturn(List.of());
        when(userRepository.findByRoleAndAssociatedDepartmentId(Role.HOD, 21L)).thenReturn(List.of());

        var result = service.getRoster("HOD_TEST");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).facultyId()).isEqualTo("FAC001");
        assertThat(result.get(0).departments()).containsExactly("Computer Science", "Information Technology");
        verify(userRepository).findByRoleAndAssociatedDepartmentId(Role.FACULTY, 20L);
        verify(userRepository).findByRoleAndAssociatedDepartmentId(Role.FACULTY, 21L);
    }

    @Test
    void rosterIncludesDepartmentHodAndIdentifiesTheirRole() {
        User departmentHod = faculty(2L, "HOD002", "Jordan HOD");
        departmentHod.setRole(Role.HOD);
        firstDepartment.setHod(departmentHod);
        when(userRepository.findByRollNumber("HOD_TEST")).thenReturn(java.util.Optional.of(hod));
        when(departmentRepository.findByHodId(10L)).thenReturn(List.of(firstDepartment));

        var result = service.getRoster("HOD_TEST");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).role()).isEqualTo(Role.HOD);
        assertThat(result.get(0).departments()).containsExactly("Computer Science");
    }

    @Test
    void rosterRejectsUsersWithoutHodRole() {
        User faculty = faculty(1L, "FAC001", "Alex Faculty");
        when(userRepository.findByRollNumber("FAC001")).thenReturn(java.util.Optional.of(faculty));

        assertThatThrownBy(() -> service.getRoster("FAC001"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only HODs");

        verifyNoInteractions(departmentRepository);
    }

    private User faculty(Long id, String rollNumber, String name) {
        User faculty = new User();
        faculty.setId(id);
        faculty.setRollNumber(rollNumber);
        faculty.setName(name);
        faculty.setRole(Role.FACULTY);
        return faculty;
    }

    private Department department(Long id, String name, String code) {
        Department department = new Department(name, code);
        department.setId(id);
        return department;
    }
}
