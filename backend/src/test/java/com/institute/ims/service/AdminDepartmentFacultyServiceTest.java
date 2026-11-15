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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDepartmentFacultyServiceTest {

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
    private AdminDepartmentFacultyService service;

    private Department department;
    private User faculty;

    @BeforeEach
    void setUp() {
        department = new Department("Computer Science", "CSE");
        department.setId(20L);
        faculty = user(10L, "FAC-010", Role.FACULTY, VerificationStatus.VERIFIED);
        when(departmentRepository.findById(20L)).thenReturn(Optional.of(department));
        ReflectionTestUtils.setField(service, "attendanceThreshold", 75.0);
        ReflectionTestUtils.setField(service, "performanceThreshold", 5.0);
    }

    @Test
    void rosterFiltersFacultyAndCalculatesAttendanceFromStudentRecords() {
        Subject subject = subject(30L, faculty);
        User firstStudent = user(1L, "STU-001", Role.STUDENT, VerificationStatus.VERIFIED);
        User secondStudent = user(2L, "STU-002", Role.STUDENT, VerificationStatus.VERIFIED);
        when(subjectRepository.findByDepartmentIdWithFacultyMembers(20L)).thenReturn(List.of(subject));
        when(userRepository.findByRoleAndAssociatedDepartmentId(Role.FACULTY, 20L)).thenReturn(List.of(faculty));
        when(userRepository.findByRoleAndDepartmentId(Role.STUDENT, 20L))
                .thenReturn(List.of(firstStudent, secondStudent));
        when(userRepository.findWithSelectedSubjectsByIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(firstStudent, secondStudent));
        when(attendanceRepository.findBySubjectIdIn(List.of(30L)))
                .thenReturn(List.of(
                        attendance(1L, firstStudent, subject, AttendanceStatus.PRESENT, 8, 1),
                        attendance(2L, firstStudent, subject, AttendanceStatus.ABSENT, 2, 2)));

        var result = service.getRoster(
                20L, 1, 30L, VerificationStatus.VERIFIED, "fac-010", "attendance", "desc", 0, 25);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.summary().totalFacultyMembers()).isEqualTo(1);
        assertThat(result.summary().activeFaculty()).isEqualTo(1);
        assertThat(result.summary().totalSubjectsAssigned()).isEqualTo(1);
        assertThat(result.summary().averageStudentAttendance()).isEqualTo(50.0);
        assertThat(result.items().get(0).facultyId()).isEqualTo("FAC-010");
        assertThat(result.items().get(0).assignedSubjects()).isEqualTo(1);
        assertThat(result.items().get(0).studentsTeaching()).isEqualTo(2);
        assertThat(result.items().get(0).averageStudentAttendance()).isEqualTo(50.0);
    }

    @Test
    void rosterSearchesFiltersSortsAndPaginatesFaculty() {
        User alice = user(11L, "FAC-011", Role.FACULTY, VerificationStatus.VERIFIED);
        alice.setName("Alice Faculty");
        User bob = user(12L, "FAC-012", Role.FACULTY, VerificationStatus.PENDING);
        bob.setName("Bob Faculty");
        when(subjectRepository.findByDepartmentIdWithFacultyMembers(20L)).thenReturn(List.of());
        when(userRepository.findByRoleAndAssociatedDepartmentId(Role.FACULTY, 20L))
                .thenReturn(List.of(alice, bob));
        when(userRepository.findByRoleAndDepartmentId(Role.STUDENT, 20L)).thenReturn(List.of());

        var firstPage = service.getRoster(
                20L, null, null, null, "faculty", "name", "desc", 0, 1);
        var verifiedOnly = service.getRoster(
                20L, null, null, VerificationStatus.VERIFIED, "alice", "facultyId", "asc", 0, 25);
        var multiSorted = service.getRoster(
                20L, null, null, null, null, "subjects:asc,name:desc", "facultyId", "asc", 0, 1);

        assertThat(firstPage.totalElements()).isEqualTo(2);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.items()).extracting(item -> item.name()).containsExactly("Bob Faculty");
        assertThat(verifiedOnly.totalElements()).isEqualTo(1);
        assertThat(verifiedOnly.items()).extracting(item -> item.facultyId()).containsExactly("FAC-011");
        assertThat(multiSorted.totalElements()).isEqualTo(2);
        assertThat(multiSorted.items()).extracting(item -> item.name()).containsExactly("Bob Faculty");
    }

    @Test
    void rosterIncludesHodsOnlyWhenTheyAreAssignedToDepartmentSubjects() {
        User assignedHod = user(13L, "HOD-013", Role.HOD, VerificationStatus.VERIFIED);
        assignedHod.setName("Assigned HOD");
        Subject subject = subject(30L, assignedHod);
        when(subjectRepository.findByDepartmentIdWithFacultyMembers(20L)).thenReturn(List.of(subject));
        when(userRepository.findByRoleAndAssociatedDepartmentId(Role.FACULTY, 20L)).thenReturn(List.of());
        when(userRepository.findByRoleAndDepartmentId(Role.STUDENT, 20L)).thenReturn(List.of());
        when(attendanceRepository.findBySubjectIdIn(List.of(30L))).thenReturn(List.of());

        var roster = service.getRoster(
                20L, null, null, null, "hod", "facultyId", "asc", 0, 25);
        var profile = service.getProfile(20L, 13L);

        assertThat(roster.totalElements()).isEqualTo(1);
        assertThat(roster.items()).extracting(item -> item.facultyId()).containsExactly("HOD-013");
        assertThat(roster.items()).extracting(item -> item.assignedSubjects()).containsExactly(1);
        assertThat(profile.personalInformation().facultyId()).isEqualTo("HOD-013");
    }

    @Test
    void profileAggregatesSubjectsAttendanceAssignmentsAndAttentionStudents() {
        Subject subject = subject(30L, faculty);
        User firstStudent = user(1L, "STU-001", Role.STUDENT, VerificationStatus.VERIFIED);
        User secondStudent = user(2L, "STU-002", Role.STUDENT, VerificationStatus.VERIFIED);
        Assignment assignment = new Assignment();
        assignment.setId(40L);
        assignment.setTitle("Project");
        assignment.setSubject(subject);
        assignment.setFaculty(faculty);
        assignment.setDueDate(LocalDate.of(2026, 12, 1));
        when(subjectRepository.findByDepartmentIdWithFacultyMembers(20L)).thenReturn(List.of(subject));
        when(userRepository.findByRoleAndAssociatedDepartmentId(Role.FACULTY, 20L)).thenReturn(List.of(faculty));
        when(userRepository.findByRoleAndDepartmentId(Role.STUDENT, 20L))
                .thenReturn(List.of(firstStudent, secondStudent));
        when(userRepository.findWithSelectedSubjectsByIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(firstStudent, secondStudent));
        when(attendanceRepository.findBySubjectIdIn(List.of(30L)))
                .thenReturn(List.of(
                        attendance(1L, firstStudent, subject, AttendanceStatus.PRESENT, 8, 1),
                        attendance(2L, firstStudent, subject, AttendanceStatus.ABSENT, 2, 2)));
        when(assignmentRepository.findByFacultyIdAndSubjectIdIn(10L, List.of(30L)))
                .thenReturn(List.of(assignment));
        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setId(50L);
        submission.setAssignment(assignment);
        submission.setStudent(firstStudent);
        submission.setStatus(SubmissionStatus.EVALUATED);
        when(submissionRepository.findByAssignmentIdIn(List.of(40L))).thenReturn(List.of(submission));

        var profile = service.getProfile(20L, 10L);

        assertThat(profile.personalInformation().facultyId()).isEqualTo("FAC-010");
        assertThat(profile.personalInformation().phone()).isNull();
        assertThat(profile.teachingOverview().totalSubjectsAssigned()).isEqualTo(1);
        assertThat(profile.teachingOverview().totalClassesConducted()).isEqualTo(2L);
        assertThat(profile.teachingOverview().totalStudentsTeaching()).isEqualTo(2);
        assertThat(profile.assignedSubjects().get(0).studentAttendancePercentage()).isEqualTo(50.0);
        assertThat(profile.attendanceOverview().totalAttendanceRecords()).isEqualTo(2);
        assertThat(profile.attendanceOverview().studentAttendancePercentage()).isEqualTo(50.0);
        assertThat(profile.assignments().get(0).totalSubmissions()).isEqualTo(1);
        assertThat(profile.assignments().get(0).pendingSubmissions()).isEqualTo(1);
        assertThat(profile.assignments().get(0).creationDate()).isNull();
        assertThat(profile.academicPerformanceOverview().averageSubjectPerformance()).isEqualTo(5.0);
        assertThat(profile.academicPerformanceOverview().studentsRequiringAttention())
                .extracting(item -> item.studentRollNumber())
                .containsExactly("STU-001");
        assertThat(profile.additionalInformation().documents()).isEmpty();
    }

    @Test
    void profileRejectsFacultyOutsideTheSelectedDepartment() {
        when(subjectRepository.findByDepartmentIdWithFacultyMembers(20L)).thenReturn(List.of());
        when(userRepository.findByRoleAndAssociatedDepartmentId(Role.FACULTY, 20L)).thenReturn(List.of());
        when(userRepository.findByRoleAndDepartmentId(Role.STUDENT, 20L)).thenReturn(List.of());
        when(userRepository.existsById(99L)).thenReturn(true);

        assertThatThrownBy(() -> service.getProfile(20L, 99L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("associated with this department");
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
        if (role == Role.STUDENT) {
            user.setYear(1);
            user.setSemester(1);
        }
        return user;
    }

    private Subject subject(Long id, User assignedFaculty) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName("Algorithms");
        subject.setCode("CS101");
        subject.setDepartment(department);
        subject.setType(SubjectType.COMPULSORY);
        subject.setAcademicYear(1);
        subject.setSemester(1);
        subject.setFacultyMembers(new HashSet<>(Set.of(assignedFaculty)));
        return subject;
    }

    private Attendance attendance(
            Long id,
            User student,
            Subject subject,
            AttendanceStatus status,
            Integer marks,
            int day) {
        Attendance attendance = new Attendance();
        attendance.setId(id);
        attendance.setStudent(student);
        attendance.setSubject(subject);
        attendance.setDate(LocalDate.of(2026, 1, day));
        attendance.setStatus(status);
        attendance.setPerformanceMarks(marks);
        return attendance;
    }
}
