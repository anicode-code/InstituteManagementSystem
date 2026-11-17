package com.institute.ims.service;

import com.institute.ims.model.Department;
import com.institute.ims.model.Role;
import com.institute.ims.model.Subject;
import com.institute.ims.model.User;
import com.institute.ims.repository.AttendanceRepository;
import com.institute.ims.repository.SubjectRepository;
import com.institute.ims.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private SubjectService subjectService;

    @Test
    void hodMayEditAssignedSubjectAttendance() {
        AttendanceService service = newService();
        User hod = user(10L, Role.HOD);
        Subject subject = subject(30L, department(20L), hod);
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject));

        service.deleteDay(30L, LocalDate.parse("2026-11-18"), hod);

        verify(attendanceRepository).deleteBySubjectIdAndDate(30L, LocalDate.parse("2026-11-18"));
    }

    @Test
    void hodCannotEditUnassignedSubjectEvenWithinTheirDepartment() {
        AttendanceService service = newService();
        User hod = user(10L, Role.HOD);
        User faculty = user(11L, Role.FACULTY);
        Subject subject = subject(30L, department(20L), faculty);
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject));

        assertThatThrownBy(() -> service.deleteDay(30L, LocalDate.parse("2026-11-18"), hod))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("subjects assigned to you");

        verify(attendanceRepository, never()).deleteBySubjectIdAndDate(anyLong(), any());
    }

    private AttendanceService newService() {
        return new AttendanceService(attendanceRepository, userRepository, subjectRepository, subjectService);
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    private Department department(Long id) {
        Department department = new Department();
        department.setId(id);
        return department;
    }

    private Subject subject(Long id, Department department, User assignedUser) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setDepartment(department);
        subject.setFacultyMembers(new HashSet<>(Set.of(assignedUser)));
        return subject;
    }
}
