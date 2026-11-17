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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDepartmentStudentsServiceTest {

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
    private AdminDepartmentStudentsService service;

    private Department department;

    @BeforeEach
    void setUp() {
        department = new Department("Computer Science", "CSE");
        department.setId(20L);
        ReflectionTestUtils.setField(service, "attendanceThreshold", 75.0);
        ReflectionTestUtils.setField(service, "performanceThreshold", 5.0);
        when(departmentRepository.existsById(20L)).thenReturn(true);
    }

    @Test
    void rosterCalculatesAttendanceFromRecordsAndAssignmentCompletion() {
        User first = student(1L, "001");
        User second = student(2L, "002");
        Subject subject = subject(30L);
        first.setSelectedSubjects(new HashSet<>(Set.of(subject)));
        second.setSelectedSubjects(new HashSet<>(Set.of(subject)));
        when(userRepository.findByRoleAndDepartmentId(Role.STUDENT, 20L))
                .thenReturn(List.of(first, second));
        when(userRepository.findWithSelectedSubjectsByIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(first, second));
        when(subjectRepository.findByDepartmentIdAndSemesterIn(20L, List.of(1)))
                .thenReturn(List.of(subject));
        when(attendanceRepository.findByStudentIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(
                        attendance(1L, first, subject, AttendanceStatus.PRESENT, 8),
                        attendance(2L, first, subject, AttendanceStatus.PRESENT, null),
                        attendance(3L, second, subject, AttendanceStatus.ABSENT, 2)));
        Assignment firstAssignment = assignment(40L, subject);
        Assignment secondAssignment = assignment(41L, subject);
        when(assignmentRepository.findBySubjectIdIn(List.of(30L)))
                .thenReturn(List.of(firstAssignment, secondAssignment));
        when(submissionRepository.findByStudentIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(submission(50L, first, firstAssignment)));

        var result = service.getRoster(
                20L, null, null, null, null, null, "rollNumber", "asc", 0, 25);

        assertThat(result.summary().totalStudents()).isEqualTo(2);
        assertThat(result.summary().averageAttendance()).isEqualTo(66.7);
        assertThat(result.summary().averageAcademicPerformance()).isEqualTo(5.0);
        assertThat(result.summary().studentsRequiringAttention()).isEqualTo(1);
        assertThat(result.students()).extracting(item -> item.rollNumber()).containsExactly("001", "002");
        assertThat(result.students().get(0).attendancePercentage()).isEqualTo(100.0);
        assertThat(result.students().get(0).submittedAssignments()).isEqualTo(1);
        assertThat(result.students().get(0).totalAssignments()).isEqualTo(2);
        assertThat(result.students().get(1).attendancePercentage()).isZero();
        assertThat(result.students().get(1).pendingAssignments()).isEqualTo(2);
        assertThat(result.students().get(1).requiresAttention()).isTrue();

        var attentionResult = service.getRoster(
                20L, null, null, null, null, "LOW_ATTENDANCE", "attendance", "desc", 0, 25);
        assertThat(attentionResult.students()).extracting(item -> item.rollNumber()).containsExactly("002");
        assertThat(attentionResult.summary().totalStudents()).isEqualTo(2);

        var secondPage = service.getRoster(
                20L, null, null, null, null, null, "rollNumber", "asc", 1, 1);
        assertThat(secondPage.students()).extracting(item -> item.rollNumber()).containsExactly("002");
    }

    @Test
    void rosterAppliesAcademicSearchAndStatusFiltersBeforeLoadingMetrics() {
        User matchingStudent = student(1L, "MATCH-001");
        User otherStudent = student(2L, "OTHER-002");
        otherStudent.setYear(2);
        otherStudent.setSemester(4);
        otherStudent.setStatus(VerificationStatus.PENDING);
        when(userRepository.findByRoleAndDepartmentId(Role.STUDENT, 20L))
                .thenReturn(List.of(matchingStudent, otherStudent));
        when(userRepository.findWithSelectedSubjectsByIdIn(List.of(1L)))
                .thenReturn(List.of(matchingStudent));
        when(attendanceRepository.findByStudentIdIn(List.of(1L))).thenReturn(List.of());

        var result = service.getRoster(
                20L, 1, 1, VerificationStatus.VERIFIED, "match-001", null, "rollNumber", "asc", 0, 25);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.students()).extracting(item -> item.rollNumber()).containsExactly("MATCH-001");
        verify(userRepository).findWithSelectedSubjectsByIdIn(List.of(1L));
    }

    @Test
    void unsupportedAttentionFilterIsRejectedForAnEmptyRoster() {
        assertThatThrownBy(() -> service.getRoster(
                20L, null, null, null, null, "unsupported", "rollNumber", "asc", 0, 25))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("attention filter");

        verify(userRepository, never()).findByRoleAndDepartmentId(Role.STUDENT, 20L);
    }

    @Test
    void missingRecordsRemainUnavailableInsteadOfBecomingZero() {
        User studentWithoutRecords = student(1L, "001");
        User studentWithRecords = student(2L, "002");
        Subject subject = subject(30L);
        studentWithoutRecords.setSelectedSubjects(new HashSet<>(Set.of(subject)));
        studentWithRecords.setSelectedSubjects(new HashSet<>(Set.of(subject)));
        when(userRepository.findByRoleAndDepartmentId(Role.STUDENT, 20L))
                .thenReturn(List.of(studentWithoutRecords, studentWithRecords));
        when(userRepository.findWithSelectedSubjectsByIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(studentWithoutRecords, studentWithRecords));
        when(subjectRepository.findByDepartmentIdAndSemesterIn(20L, List.of(1))).thenReturn(List.of());
        when(attendanceRepository.findByStudentIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(attendance(
                        1L, studentWithRecords, subject, AttendanceStatus.PRESENT, 8)));
        when(assignmentRepository.findBySubjectIdIn(List.of(30L))).thenReturn(List.of());

        var result = service.getRoster(
                20L, null, null, null, null, null, "performance", "desc", 0, 25);

        assertThat(result.summary().averageAttendance()).isEqualTo(100.0);
        assertThat(result.summary().averageAcademicPerformance()).isEqualTo(8.0);
        assertThat(result.students()).extracting(item -> item.rollNumber()).containsExactly("002", "001");
        var noRecords = result.students().get(1);
        assertThat(noRecords.attendancePercentage()).isNull();
        assertThat(noRecords.academicPerformance()).isNull();
        assertThat(noRecords.lowAttendance()).isFalse();
        assertThat(noRecords.poorPerformance()).isFalse();
    }

    @Test
    void rosterAppliesSecondarySortsWhenPrimaryValuesAreEqual() {
        User alice = student(1L, "001");
        alice.setName("Alice");
        User bob = student(2L, "002");
        bob.setName("Bob");
        User charlie = student(3L, "003");
        charlie.setName("Charlie");
        when(userRepository.findByRoleAndDepartmentId(Role.STUDENT, 20L))
                .thenReturn(List.of(alice, bob, charlie));
        when(userRepository.findWithSelectedSubjectsByIdIn(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(alice, bob, charlie));
        when(attendanceRepository.findByStudentIdIn(List.of(1L, 2L, 3L))).thenReturn(List.of());

        var result = service.getRoster(
                20L, null, null, null, null, null, "attendance:asc,name:desc", "rollNumber", "asc", 0, 1);

        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.students()).extracting(item -> item.name()).containsExactly("Charlie");
    }

    @Test
    void adminCannotReadAStudentOutsideTheSelectedDepartment() {
        Department anotherDepartment = new Department("Information Technology", "IT");
        anotherDepartment.setId(21L);
        User foreignStudent = student(9L, "FOREIGN");
        foreignStudent.setDepartment(anotherDepartment);
        when(userRepository.findById(9L)).thenReturn(Optional.of(foreignStudent));

        assertThatThrownBy(() -> service.getStudentProfile(20L, 9L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("this department");

        verify(userRepository, never()).findWithSelectedSubjectsByIdIn(List.of(9L));
    }

    @Test
    void adminStudentProfileShowsEditableRegistrationDetailsAndGpaHistory() {
        User student = student(1L, "001");
        student.setRegistrationNumber("REG-001");
        student.setYear(2);
        student.setSemester(3);
        student.setPhone("9876543210");
        student.setDateOfBirth(LocalDate.of(2002, 4, 12));
        student.setResidentialAddress("12 Campus Road");
        student.setSemesterSgpas(Map.of(1, 8.0, 2, 9.0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(userRepository.findWithSelectedSubjectsByIdIn(List.of(1L))).thenReturn(List.of(student));
        when(subjectRepository.findByDepartmentIdAndSemesterIn(20L, List.of(3))).thenReturn(List.of());
        when(attendanceRepository.findByStudentIdIn(List.of(1L))).thenReturn(List.of());

        var profile = service.getStudentProfile(20L, 1L);

        assertThat(profile.personalInformation().phone()).isEqualTo("9876543210");
        assertThat(profile.personalInformation().dateOfBirth()).isEqualTo(LocalDate.of(2002, 4, 12));
        assertThat(profile.personalInformation().address()).isEqualTo("12 Campus Road");
        assertThat(profile.academicInformation().registrationNumber()).isEqualTo("REG-001");
        assertThat(profile.semesterGpas()).containsExactly(
                new com.institute.ims.dto.AdminStudentProfile.SemesterGpa(1, 8.0),
                new com.institute.ims.dto.AdminStudentProfile.SemesterGpa(2, 9.0));
        assertThat(profile.yearlyGpas()).containsExactly(
                new com.institute.ims.dto.AdminStudentProfile.YearlyGpa(1, 8.5));
    }

    private User student(Long id, String rollNumber) {
        User student = new User();
        student.setId(id);
        student.setName("Student " + rollNumber);
        student.setRollNumber(rollNumber);
        student.setRole(Role.STUDENT);
        student.setStatus(VerificationStatus.VERIFIED);
        student.setDepartment(department);
        student.setYear(1);
        student.setSemester(1);
        return student;
    }

    private Subject subject(Long id) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName("Programming");
        subject.setCode("CS101");
        subject.setSemester(1);
        subject.setType(SubjectType.COMPULSORY);
        subject.setDepartment(department);
        return subject;
    }

    private Attendance attendance(
            Long id,
            User student,
            Subject subject,
            AttendanceStatus status,
            Integer performanceMarks) {
        Attendance attendance = new Attendance();
        attendance.setId(id);
        attendance.setStudent(student);
        attendance.setSubject(subject);
        attendance.setDate(LocalDate.of(2026, 1, id.intValue()));
        attendance.setStatus(status);
        attendance.setPerformanceMarks(performanceMarks);
        return attendance;
    }

    private Assignment assignment(Long id, Subject subject) {
        Assignment assignment = new Assignment();
        assignment.setId(id);
        assignment.setTitle("Assignment " + id);
        assignment.setSubject(subject);
        assignment.setMaxMarks(100);
        return assignment;
    }

    private AssignmentSubmission submission(Long id, User student, Assignment assignment) {
        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setId(id);
        submission.setStudent(student);
        submission.setAssignment(assignment);
        submission.setStatus(SubmissionStatus.EVALUATED);
        return submission;
    }
}
