package com.institute.ims.service;

import com.institute.ims.model.Department;
import com.institute.ims.model.Attendance;
import com.institute.ims.model.AttendanceStatus;
import com.institute.ims.model.Role;
import com.institute.ims.model.Subject;
import com.institute.ims.model.User;
import com.institute.ims.repository.AssignmentRepository;
import com.institute.ims.repository.AssignmentSubmissionRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HodStudentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SubjectService subjectService;

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentSubmissionRepository submissionRepository;

    @InjectMocks
    private HodStudentService service;

    private User hod;
    private Department assignedDepartment;

    @BeforeEach
    void setUp() {
        hod = new User();
        hod.setId(10L);
        hod.setRollNumber("HOD_TEST");
        hod.setRole(Role.HOD);

        assignedDepartment = new Department();
        assignedDepartment.setId(20L);
        assignedDepartment.setName("Computer Science");
        assignedDepartment.setCode("CSE");

    }

    @Test
    void rosterFiltersByRequestedYearAndAssignedDepartmentAndDefaultsToRollOrder() {
        stubHodScope();
        User first = student(1L, "001", assignedDepartment, 2);
        User second = student(2L, "002", assignedDepartment, 2);
        when(userRepository.findByRoleAndYearAndDepartmentIdInOrderByRollNumberAsc(
                Role.STUDENT, 2, List.of(20L))).thenReturn(List.of(first, second));
        when(subjectService.getByStudent(anyLong())).thenReturn(List.of());
        when(attendanceService.getByStudent(anyLong())).thenReturn(List.of());
        when(submissionRepository.findByStudentId(anyLong())).thenReturn(List.of());

        var result = service.getRoster("HOD_TEST", 2, "rollNumber", "asc");

        assertThat(result).extracting(item -> item.rollNumber()).containsExactly("001", "002");
        assertThat(result).allSatisfy(item -> {
            assertThat(item.department().id()).isEqualTo(20L);
            assertThat(item.attendancePercentage()).isNull();
            assertThat(item.classPerformance()).isNull();
            assertThat(item.pendingAssignments()).isZero();
        });
        verify(userRepository).findByRoleAndYearAndDepartmentIdInOrderByRollNumberAsc(
                Role.STUDENT, 2, List.of(20L));
    }

    @Test
    void rosterRejectsAcademicYearsOutsideFirstThroughFourth() {
        assertThatThrownBy(() -> service.getRoster("HOD_TEST", 5, "rollNumber", "asc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1 and 4");

        verifyNoInteractions(departmentRepository);
    }

    @Test
    void rosterSortsByAttendanceInRequestedDirection() {
        stubHodScope();
        User presentStudent = student(1L, "001", assignedDepartment, 1);
        User absentStudent = student(2L, "002", assignedDepartment, 1);
        when(userRepository.findByRoleAndYearAndDepartmentIdInOrderByRollNumberAsc(
                Role.STUDENT, 1, List.of(20L))).thenReturn(List.of(presentStudent, absentStudent));

        Subject subject = new Subject();
        subject.setId(30L);
        subject.setDepartment(assignedDepartment);
        when(subjectService.getByStudent(anyLong())).thenReturn(List.of(subject));
        when(attendanceService.getByStudent(1L)).thenReturn(List.of(attendance(subject, AttendanceStatus.PRESENT, 8)));
        when(attendanceService.getByStudent(2L)).thenReturn(List.of(attendance(subject, AttendanceStatus.ABSENT, 2)));
        when(assignmentRepository.findBySubjectIdIn(List.of(30L))).thenReturn(List.of());
        when(submissionRepository.findByStudentId(anyLong())).thenReturn(List.of());

        var result = service.getRoster("HOD_TEST", 1, "attendancePercentage", "desc");

        assertThat(result).extracting(item -> item.rollNumber()).containsExactly("001", "002");
        assertThat(result).extracting(item -> item.attendancePercentage()).containsExactly(100.0, 0.0);
    }

    @Test
    void detailsRejectStudentsOutsideAssignedDepartment() {
        stubHodScope();
        Department otherDepartment = new Department();
        otherDepartment.setId(21L);
        User otherDepartmentStudent = student(5L, "FOREIGN", otherDepartment, 1);
        when(userRepository.findById(5L)).thenReturn(Optional.of(otherDepartmentStudent));

        assertThatThrownBy(() -> service.getStudentDetails("HOD_TEST", 5L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("assigned department");

        verifyNoInteractions(subjectService, attendanceService, assignmentRepository, submissionRepository);
    }

    private User student(Long id, String rollNumber, Department department, int year) {
        User student = new User();
        student.setId(id);
        student.setName("Student " + rollNumber);
        student.setRollNumber(rollNumber);
        student.setRole(Role.STUDENT);
        student.setDepartment(department);
        student.setYear(year);
        return student;
    }

    private Attendance attendance(Subject subject, AttendanceStatus status, int marks) {
        Attendance attendance = new Attendance();
        attendance.setSubject(subject);
        attendance.setStatus(status);
        attendance.setPerformanceMarks(marks);
        return attendance;
    }

    private void stubHodScope() {
        when(userRepository.findByRollNumber("HOD_TEST")).thenReturn(Optional.of(hod));
        when(departmentRepository.findByHodId(10L)).thenReturn(List.of(assignedDepartment));
    }
}
