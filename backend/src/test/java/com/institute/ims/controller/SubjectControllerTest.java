package com.institute.ims.controller;

import com.institute.ims.model.Department;
import com.institute.ims.model.Role;
import com.institute.ims.model.Subject;
import com.institute.ims.model.User;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.service.SubjectService;
import com.institute.ims.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubjectControllerTest {

    @Mock
    private SubjectService subjectService;
    @Mock
    private UserService userService;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private Authentication authentication;

    private SubjectController controller;

    @BeforeEach
    void setUp() {
        controller = new SubjectController(subjectService, userService, departmentRepository);
        when(authentication.getName()).thenReturn("HOD_TEST");
    }

    @Test
    void hodAttendanceSubjectsIncludeAllSemestersWithinTheirDepartmentOnly() {
        Department department = new Department();
        department.setId(20L);
        User hod = new User();
        hod.setRole(Role.HOD);
        hod.setDepartment(department);
        when(userService.getUserByRollNumber("HOD_TEST")).thenReturn(hod);

        Subject firstSemesterSubject = subject(1L, department, 1);
        Subject eighthSemesterSubject = subject(2L, department, 8);
        List<Subject> departmentSubjects = List.of(firstSemesterSubject, eighthSemesterSubject);
        when(subjectService.getByDepartment(20L)).thenReturn(departmentSubjects);

        var response = controller.getAllForAttendance(authentication);

        assertThat(response.getBody()).containsExactlyElementsOf(departmentSubjects);
        assertThat(response.getBody()).extracting(Subject::getSemester).containsExactly(1, 8);
        verify(subjectService).getByDepartment(20L);
        verify(subjectService, never()).getAll();
    }

    private Subject subject(Long id, Department department, int semester) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setDepartment(department);
        subject.setSemester(semester);
        return subject;
    }
}
