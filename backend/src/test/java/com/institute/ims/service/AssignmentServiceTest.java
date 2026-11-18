package com.institute.ims.service;

import com.institute.ims.model.Assignment;
import com.institute.ims.model.AssignmentSubmission;
import com.institute.ims.model.Department;
import com.institute.ims.model.Role;
import com.institute.ims.model.Subject;
import com.institute.ims.model.User;
import com.institute.ims.repository.AssignmentAttachmentRepository;
import com.institute.ims.repository.AssignmentRepository;
import com.institute.ims.repository.AssignmentSubmissionRepository;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.SubmissionAttachmentRepository;
import com.institute.ims.repository.SubjectRepository;
import com.institute.ims.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private AssignmentAttachmentRepository assignmentAttachmentRepository;
    @Mock
    private AssignmentSubmissionRepository submissionRepository;
    @Mock
    private SubmissionAttachmentRepository submissionAttachmentRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DepartmentRepository departmentRepository;

    private AssignmentService service;

    @BeforeEach
    void setUp() {
        service = new AssignmentService(
                assignmentRepository,
                assignmentAttachmentRepository,
                submissionRepository,
                submissionAttachmentRepository,
                subjectRepository,
                userRepository,
                departmentRepository);
    }

    @Test
    void hodAssignmentListIsScopedToTheirDepartmentAcrossSemesters() {
        User hod = user(10L, Role.HOD);
        Department firstDepartment = department(20L);
        hod.setDepartment(firstDepartment);
        List<Assignment> assignments = List.of(new Assignment(), new Assignment());
        when(assignmentRepository.findBySubjectDepartmentId(20L)).thenReturn(assignments);

        assertThat(service.getScopedAssignments(hod)).containsExactlyElementsOf(assignments);

        verify(assignmentRepository).findBySubjectDepartmentId(20L);
        verify(assignmentRepository, never()).findAll();
    }

    @Test
    void hodWithoutAnAssignedDepartmentSeesNoAssignments() {
        User hod = user(10L, Role.HOD);
        when(departmentRepository.findByHodId(10L)).thenReturn(List.of());

        assertThat(service.getScopedAssignments(hod)).isEmpty();

        verifyNoInteractions(assignmentRepository);
    }

    @Test
    void hodCanCreateAssignmentForSubjectAssignedToThem() {
        User hod = user(10L, Role.HOD);
        Subject subject = subject(30L, department(20L), hod);
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject));
        when(userRepository.findById(10L)).thenReturn(Optional.of(hod));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Assignment assignment = service.create(
                "Assignment", "Instructions", LocalDate.parse("2026-12-01"), 100, 30L, 10L, hod);

        assertThat(assignment.getSubject()).isSameAs(subject);
        verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    void hodCannotCreateAssignmentForSubjectNotAssignedToThem() {
        User hod = user(10L, Role.HOD);
        Subject subject = subject(30L, department(20L), user(11L, Role.FACULTY));
        when(subjectRepository.findById(30L)).thenReturn(Optional.of(subject));
        when(userRepository.findById(10L)).thenReturn(Optional.of(hod));

        assertThatThrownBy(() -> service.create(
                "Assignment", "Instructions", LocalDate.parse("2026-12-01"), 100, 30L, 10L, hod))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("subjects assigned to you");

        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    @Test
    void studentCanSubmitFilesWithoutText() {
        Assignment assignment = new Assignment();
        User student = user(40L, Role.STUDENT);
        when(assignmentRepository.findById(30L)).thenReturn(Optional.of(assignment));
        when(userRepository.findById(40L)).thenReturn(Optional.of(student));
        when(submissionRepository.findByAssignmentIdAndStudentId(30L, 40L)).thenReturn(Optional.empty());
        when(submissionRepository.save(any(AssignmentSubmission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile file = new MockMultipartFile(
                "files", "answer.txt", "text/plain", "answer".getBytes());

        AssignmentSubmission submission = service.submitAssignment(
                30L, 40L, "", new MockMultipartFile[]{file});

        assertThat(submission.getSubmissionText()).isEmpty();
        assertThat(submission.getAttachments()).hasSize(1);
        assertThat(submission.getAttachments().get(0).getFileName()).isEqualTo("answer.txt");
    }

    @Test
    void studentCannotSubmitWithoutTextOrFiles() {
        when(assignmentRepository.findById(30L)).thenReturn(Optional.of(new Assignment()));
        when(userRepository.findById(40L)).thenReturn(Optional.of(user(40L, Role.STUDENT)));

        assertThatThrownBy(() -> service.submitAssignment(30L, 40L, " ", new MockMultipartFile[0]))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Provide an answer");

        verify(submissionRepository, never()).save(any(AssignmentSubmission.class));
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
