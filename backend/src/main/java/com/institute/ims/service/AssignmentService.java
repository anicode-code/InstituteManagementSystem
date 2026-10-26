package com.institute.ims.service;

import com.institute.ims.model.*;
import com.institute.ims.repository.AssignmentRepository;
import com.institute.ims.repository.AssignmentSubmissionRepository;
import com.institute.ims.repository.SubjectRepository;
import com.institute.ims.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            SubjectRepository subjectRepository,
            UserRepository userRepository) {

        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
    }

    public List<Assignment> getAll() {
        return assignmentRepository.findAll();
    }

    public List<Assignment> getScopedAssignments(User user) {
        if (user.getRole() == Role.FACULTY) {
            return assignmentRepository.findBySubjectFacultyId(user.getId());
        } else if (user.getRole() == Role.HOD && user.getDepartment() != null) {
            return assignmentRepository.findBySubjectDepartmentId(user.getDepartment().getId());
        }
        return assignmentRepository.findAll();
    }

    public Assignment getById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
    }

    public List<Assignment> getByFaculty(Long facultyId) {
        return assignmentRepository.findByFacultyId(facultyId);
    }

    public List<Assignment> getBySubject(Long subjectId) {
        return assignmentRepository.findBySubjectId(subjectId);
    }

    @Transactional
    public Assignment create(
            String title,
            String description,
            LocalDate dueDate,
            Integer maxMarks,
            Long subjectId,
            Long facultyId,
            User requestingUser) {

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        User faculty = userRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        // RBAC validation: Faculty can only create assignments for their assigned subjects
        if (requestingUser.getRole() == Role.FACULTY) {
            if (!isFacultyAssignedToSubject(subject, requestingUser)) {
                throw new RuntimeException("You can only create assignments for your assigned subjects.");
            }
        } else if (requestingUser.getRole() == Role.STUDENT) {
            throw new RuntimeException("Students cannot create assignments.");
        }

        Assignment assignment = new Assignment();
        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setDueDate(dueDate);
        assignment.setMaxMarks(maxMarks != null ? maxMarks : 100);
        assignment.setSubject(subject);
        assignment.setFaculty(faculty);

        return assignmentRepository.save(assignment);
    }

    @Transactional
    public void delete(Long id, User requestingUser) {
        Assignment assignment = getById(id);

        if (requestingUser.getRole() == Role.FACULTY) {
            if (!isFacultyAssignedToSubject(assignment.getSubject(), requestingUser)) {
                throw new RuntimeException("You can only delete your own assignments.");
            }
        } else if (requestingUser.getRole() == Role.STUDENT) {
            throw new RuntimeException("Students cannot delete assignments.");
        }

        assignmentRepository.delete(assignment);
    }

    // ──────── Submissions & Evaluation ────────

    public AssignmentSubmission submitAssignment(
            Long assignmentId,
            Long studentId,
            String submissionText) {

        Assignment assignment = getById(assignmentId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (student.getRole() != Role.STUDENT) {
            throw new RuntimeException("Only students can submit assignments.");
        }

        if (submissionText == null || submissionText.trim().isEmpty()) {
            throw new RuntimeException("Submission content/link cannot be empty.");
        }

        SubmissionStatus status = SubmissionStatus.SUBMITTED;
        if (assignment.getDueDate() != null && LocalDate.now().isAfter(assignment.getDueDate())) {
            status = SubmissionStatus.LATE;
        }

        Optional<AssignmentSubmission> existing = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, studentId);

        AssignmentSubmission submission = existing.orElseGet(AssignmentSubmission::new);
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setSubmissionText(submissionText.trim());
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus(status);

        return submissionRepository.save(submission);
    }

    public Optional<AssignmentSubmission> getMySubmission(Long assignmentId, Long studentId) {
        return submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmission> getSubmissionsForAssignment(
            Long assignmentId,
            User requestingUser) {

        Assignment assignment = getById(assignmentId);

        // RBAC: Faculty can only view submissions for their own assignments
        if (requestingUser.getRole() == Role.FACULTY) {
            if (!isFacultyAssignedToSubject(assignment.getSubject(), requestingUser)) {
                throw new RuntimeException("You can only view submissions for your own assignments.");
            }
        } else if (requestingUser.getRole() == Role.STUDENT) {
            throw new RuntimeException("Students cannot view other submissions.");
        }

        return submissionRepository.findByAssignmentId(assignmentId);
    }

    @Transactional
    public AssignmentSubmission evaluateSubmission(
            Long submissionId,
            Integer marks,
            String feedback,
            User requestingUser) {

        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        Assignment assignment = submission.getAssignment();

        // RBAC: Only the assignment's faculty (or Admin) can evaluate
        if (requestingUser.getRole() == Role.FACULTY) {
            if (!isFacultyAssignedToSubject(assignment.getSubject(), requestingUser)) {
                throw new RuntimeException("You can only grade submissions for your own assignments.");
            }
        } else if (requestingUser.getRole() == Role.STUDENT) {
            throw new RuntimeException("Unauthorized.");
        }

        if (marks != null) {
            if (marks < 0 || (assignment.getMaxMarks() != null && marks > assignment.getMaxMarks())) {
                throw new RuntimeException("Marks must be between 0 and " + assignment.getMaxMarks());
            }
            submission.setMarks(marks);
            submission.setStatus(SubmissionStatus.EVALUATED);
        }

        if (feedback != null) {
            submission.setFeedback(feedback);
        }

        return submissionRepository.save(submission);
    }

    private boolean isFacultyAssignedToSubject(Subject subject, User faculty) {
        return subject.getFacultyMembers().stream()
                .anyMatch(member -> member.getId().equals(faculty.getId()));
    }
}
