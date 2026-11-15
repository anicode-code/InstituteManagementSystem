package com.institute.ims.service;

import com.institute.ims.model.*;
import com.institute.ims.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDepartmentHodServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentSubmissionRepository submissionRepository;

    @InjectMocks
    private AdminDepartmentHodService service;

    private Department department;
    private User hod;

    @BeforeEach
    void setUp() {
        department = new Department("Computer Science", "CSE");
        department.setId(20L);
        hod = user(10L, "HOD-010", Role.HOD, VerificationStatus.VERIFIED);
        department.setHod(hod);
        when(departmentRepository.findById(20L)).thenReturn(Optional.of(department));
        ReflectionTestUtils.setField(service, "attendanceThreshold", 75.0);
        ReflectionTestUtils.setField(service, "performanceThreshold", 5.0);
    }

    @Test
    void directoryReturnsTheCurrentDepartmentHodAndDepartmentSummary() {
        User firstStudent = user(1L, "STU-001", Role.STUDENT, VerificationStatus.VERIFIED);
        firstStudent.setYear(1);
        User secondStudent = user(2L, "STU-002", Role.STUDENT, VerificationStatus.VERIFIED);
        secondStudent.setYear(2);
        when(userRepository.findByRoleAndDepartmentId(Role.STUDENT, 20L))
                .thenReturn(List.of(firstStudent, secondStudent));
        when(subjectRepository.findByDepartmentIdWithFacultyMembers(20L)).thenReturn(List.of());
        when(userRepository.findByRoleAndAssociatedDepartmentId(Role.FACULTY, 20L)).thenReturn(List.of());

        var result = service.getDirectory(20L);

        assertThat(result.currentHod().hodId()).isEqualTo("HOD-010");
        assertThat(result.summary().totalStudents()).isEqualTo(2);
    }

    @Test
    void directoryShowsNoCurrentHodWhenNoneIsAssigned() {
        department.setHod(null);
        when(userRepository.findByRoleAndDepartmentId(Role.STUDENT, 20L)).thenReturn(List.of());
        when(subjectRepository.findByDepartmentIdWithFacultyMembers(20L)).thenReturn(List.of());
        when(userRepository.findByRoleAndAssociatedDepartmentId(Role.FACULTY, 20L)).thenReturn(List.of());

        var result = service.getDirectory(20L);

        assertThat(result.currentHod()).isNull();
        assertThat(result.summary().totalStudents()).isZero();
    }

    @Test
    void profileRejectsHodsNotCurrentlyAssignedToTheDepartment() {
        when(userRepository.existsById(99L)).thenReturn(true);

        assertThatThrownBy(() -> service.getProfile(20L, 99L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("current HOD");
    }

    @Test
    void profileIncludesTheCurrentHodInFacultyOverviewOnlyWhenTheyTeachASubject() {
        Subject subject = subject(40L, hod);
        when(userRepository.findByRoleAndDepartmentId(Role.STUDENT, 20L)).thenReturn(List.of());
        when(subjectRepository.findByDepartmentIdWithFacultyMembers(20L)).thenReturn(List.of(subject));
        when(userRepository.findByRoleAndAssociatedDepartmentId(Role.FACULTY, 20L)).thenReturn(List.of());
        when(attendanceRepository.findBySubjectIdIn(List.of(40L))).thenReturn(List.of());
        when(assignmentRepository.findBySubjectIdIn(List.of(40L))).thenReturn(List.of());

        var profile = service.getProfile(20L, 10L);

        assertThat(profile.departmentOverview().totalFacultyMembers()).isEqualTo(1);
        assertThat(profile.facultyMembers()).extracting(item -> item.userId()).containsExactly(10L);
        assertThat(profile.facultyMembers().get(0).assignedYears()).containsExactly(1);
    }

    @Test
    void profileSubjectsAreOrderedBySemesterThenName() {
        Subject semesterThree = subject(41L, hod);
        semesterThree.setName("Zeta");
        semesterThree.setSemester(3);
        Subject semesterOneZ = subject(42L, hod);
        semesterOneZ.setName("Zeta");
        Subject semesterOneA = subject(40L, hod);
        semesterOneA.setName("Alpha");
        when(userRepository.findByRoleAndDepartmentId(Role.STUDENT, 20L)).thenReturn(List.of());
        when(subjectRepository.findByDepartmentIdWithFacultyMembers(20L))
                .thenReturn(List.of(semesterThree, semesterOneZ, semesterOneA));
        when(userRepository.findByRoleAndAssociatedDepartmentId(Role.FACULTY, 20L)).thenReturn(List.of());
        when(attendanceRepository.findBySubjectIdIn(anyList())).thenReturn(List.of());
        when(assignmentRepository.findBySubjectIdIn(anyList())).thenReturn(List.of());

        var profile = service.getProfile(20L, 10L);

        assertThat(profile.subjects()).extracting(subject -> subject.subjectId())
                .containsExactly(40L, 42L, 41L);
    }

    @Test
    void profileCalculatesAttendanceAndPendingAssignmentsFromExistingRecords() {
        User faculty = user(30L, "FAC-030", Role.FACULTY, VerificationStatus.VERIFIED);
        User student = user(1L, "STU-001", Role.STUDENT, VerificationStatus.VERIFIED);
        student.setYear(1);
        student.setSemester(1);
        Subject subject = subject(40L, faculty);
        student.setSelectedSubjects(new HashSet<>(Set.of(subject)));

        Assignment assignment = new Assignment();
        assignment.setId(50L);
        assignment.setTitle("Project");
        assignment.setSubject(subject);
        assignment.setFaculty(faculty);
        assignment.setDueDate(LocalDate.now().plusDays(10));

        when(userRepository.findByRoleAndDepartmentId(Role.STUDENT, 20L)).thenReturn(List.of(student));
        when(userRepository.findWithSelectedSubjectsByIdIn(List.of(1L))).thenReturn(List.of(student));
        when(subjectRepository.findByDepartmentIdWithFacultyMembers(20L)).thenReturn(List.of(subject));
        when(userRepository.findByRoleAndAssociatedDepartmentId(Role.FACULTY, 20L)).thenReturn(List.of(faculty));
        when(attendanceRepository.findBySubjectIdIn(List.of(40L))).thenReturn(List.of(
                attendance(student, subject, AttendanceStatus.PRESENT, 8),
                attendance(student, subject, AttendanceStatus.ABSENT, null)));
        when(assignmentRepository.findBySubjectIdIn(List.of(40L))).thenReturn(List.of(assignment));
        when(submissionRepository.findByAssignmentIdIn(List.of(50L))).thenReturn(List.of());

        var profile = service.getProfile(20L, 10L);

        assertThat(profile.departmentOverview().totalStudents()).isEqualTo(1);
        assertThat(profile.studentOverview().overallAttendance()).isEqualTo(50.0);
        assertThat(profile.attendanceOverview().departmentAttendancePercentage()).isEqualTo(50.0);
        assertThat(profile.subjects().get(0).attendancePercentage()).isEqualTo(50.0);
        assertThat(profile.subjects().get(0).classesConducted()).isEqualTo(1);
        assertThat(profile.assignmentOverview().totalAssignments()).isEqualTo(1);
        assertThat(profile.assignmentOverview().totalActiveAssignments()).isEqualTo(1);
        assertThat(profile.assignmentOverview().pendingSubmissions()).isEqualTo(1);
        assertThat(profile.assignmentOverview().assignments().get(0).gradingStatus()).isEqualTo("NO SUBMISSIONS");
        assertThat(profile.studentOverview().studentsRequiringAttention().get(0).reasons())
                .containsExactly("LOW ATTENDANCE");
    }

    @Test
    void profileUsesUnavailableValuesWhenTheDepartmentHasNoRecords() {
        when(userRepository.findByRoleAndDepartmentId(Role.STUDENT, 20L)).thenReturn(List.of());
        when(subjectRepository.findByDepartmentIdWithFacultyMembers(20L)).thenReturn(List.of());
        when(userRepository.findByRoleAndAssociatedDepartmentId(Role.FACULTY, 20L)).thenReturn(List.of());

        var profile = service.getProfile(20L, 10L);

        assertThat(profile.departmentOverview().departmentAttendanceAverage()).isNull();
        assertThat(profile.departmentOverview().overallAcademicPerformance()).isNull();
        assertThat(profile.studentOverview().overallAttendance()).isNull();
        assertThat(profile.academicPerformanceOverview().departmentAveragePerformance()).isNull();
    }

    private User user(Long id, String rollNumber, Role role, VerificationStatus status) {
        User user = new User();
        user.setId(id);
        user.setName("User " + rollNumber);
        user.setRollNumber(rollNumber);
        user.setEmail(rollNumber.toLowerCase() + "@example.test");
        user.setRole(role);
        user.setStatus(status);
        user.setDepartment(department);
        return user;
    }

    private Subject subject(Long id, User faculty) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName("Algorithms");
        subject.setCode("CS101");
        subject.setDepartment(department);
        subject.setType(SubjectType.COMPULSORY);
        subject.setAcademicYear(1);
        subject.setSemester(1);
        subject.setFacultyMembers(new HashSet<>(Set.of(faculty)));
        return subject;
    }

    private Attendance attendance(User student, Subject subject, AttendanceStatus status, Integer marks) {
        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setSubject(subject);
        attendance.setDate(LocalDate.now());
        attendance.setStatus(status);
        attendance.setPerformanceMarks(marks);
        return attendance;
    }
}
