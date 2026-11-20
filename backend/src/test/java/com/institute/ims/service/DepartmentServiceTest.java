package com.institute.ims.service;

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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DepartmentService service;

    @Test
    void assigningHodSetsAdministrativeRoleAndClearsReplacedHod() {
        Department department = department(10L);
        User previousHod = user(1L, Role.HOD, "HOD");
        User faculty = user(2L, Role.FACULTY, "NONE");
        previousHod.setDepartments(Set.of(department));
        faculty.setDepartments(Set.of(department));
        department.setHod(previousHod);
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(userRepository.findById(2L)).thenReturn(Optional.of(faculty));
        when(departmentRepository.findByHodId(1L)).thenReturn(List.of());
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.assignHod(10L, 2L);

        assertThat(previousHod.getRole()).isEqualTo(Role.FACULTY);
        assertThat(previousHod.getAdministrativeRole()).isEqualTo("NONE");
        assertThat(faculty.getRole()).isEqualTo(Role.HOD);
        assertThat(faculty.getAdministrativeRole()).isEqualTo("HOD");
        assertThat(department.getHod()).isSameAs(faculty);
    }

    @Test
    void removingLastHodPositionResetsAdministrativeRole() {
        Department department = department(10L);
        User previousHod = user(1L, Role.HOD, "HOD");
        department.setHod(previousHod);
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(departmentRepository.findByHodId(1L)).thenReturn(List.of());
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.removeHod(10L);

        assertThat(department.getHod()).isNull();
        assertThat(previousHod.getRole()).isEqualTo(Role.FACULTY);
        assertThat(previousHod.getAdministrativeRole()).isEqualTo("NONE");
    }

    @Test
    void removingOneOfSeveralHodPositionsKeepsAdministrativeRoleAsHod() {
        Department department = department(10L);
        Department otherDepartment = department(11L);
        User previousHod = user(1L, Role.HOD, "HOD");
        department.setHod(previousHod);
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(departmentRepository.findByHodId(1L)).thenReturn(List.of(otherDepartment));
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.removeHod(10L);

        assertThat(previousHod.getRole()).isEqualTo(Role.HOD);
        assertThat(previousHod.getAdministrativeRole()).isEqualTo("HOD");
    }

    private Department department(Long id) {
        Department department = new Department("Department " + id, "D" + id);
        department.setId(id);
        return department;
    }

    private User user(Long id, Role role, String administrativeRole) {
        User user = new User();
        user.setId(id);
        user.setRollNumber("USER" + id);
        user.setRole(role);
        user.setStatus(VerificationStatus.VERIFIED);
        user.setAdministrativeRole(administrativeRole);
        return user;
    }
}
