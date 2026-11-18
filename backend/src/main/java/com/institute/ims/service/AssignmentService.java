package com.institute.ims.service;

import com.institute.ims.dto.AttachmentFile;
import com.institute.ims.dto.AttachmentMetadata;
import com.institute.ims.model.*;
import com.institute.ims.repository.AssignmentAttachmentRepository;
import com.institute.ims.repository.AssignmentRepository;
import com.institute.ims.repository.AssignmentSubmissionRepository;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.SubmissionAttachmentRepository;
import com.institute.ims.repository.SubjectRepository;
import com.institute.ims.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentAttachmentRepository assignmentAttachmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final SubmissionAttachmentRepository submissionAttachmentRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            AssignmentAttachmentRepository assignmentAttachmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            SubmissionAttachmentRepository submissionAttachmentRepository,
            SubjectRepository subjectRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository) {

        this.assignmentRepository = assignmentRepository;
        this.assignmentAttachmentRepository = assignmentAttachmentRepository;
        this.submissionRepository = submissionRepository;
        this.submissionAttachmentRepository = submissionAttachmentRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<Assignment> getAll() {
        return assignmentRepository.findAll();
    }

    public List<Assignment> getScopedAssignments(User user) {
        if (user.getRole() == Role.FACULTY) {
            return assignmentRepository.findBySubjectFacultyId(user.getId());
        } else if (user.getRole() == Role.HOD) {
            Department department = user.getDepartment();
            if (department == null) {
                department = departmentRepository.findByHodId(user.getId()).stream()
                        .findFirst()
                        .orElse(null);
            }
            return department == null
                    ? List.of()
                    : assignmentRepository.findBySubjectDepartmentId(department.getId());
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
        } else if (requestingUser.getRole() == Role.HOD && !isFacultyAssignedToSubject(subject, requestingUser)) {
            throw new RuntimeException("You can only create assignments for subjects assigned to you.");
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
    public Assignment createWithAttachments(
            String title,
            String description,
            LocalDate dueDate,
            Integer maxMarks,
            Long subjectId,
            Long facultyId,
            User requestingUser,
            MultipartFile[] files) {

        Assignment assignment = create(title, description, dueDate, maxMarks, subjectId, facultyId, requestingUser);
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    assignment.addAttachment(toAssignmentAttachment(file));
                }
            }
        }
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public Assignment update(
            Long id,
            String title,
            String description,
            LocalDate dueDate,
            Integer maxMarks,
            Long subjectId,
            User requestingUser,
            MultipartFile[] files) {

        Assignment assignment = getById(id);
        if (requestingUser.getRole() == Role.FACULTY || requestingUser.getRole() == Role.HOD) {
            if (!assignment.getFaculty().getId().equals(requestingUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only edit assignments you created.");
            }
        } else if (requestingUser.getRole() == Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Students cannot edit assignments.");
        }

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));
        if (!isFacultyAssignedToSubject(subject, requestingUser)
                && requestingUser.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only edit assignments for subjects assigned to you.");
        }
        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setDueDate(dueDate);
        assignment.setMaxMarks(maxMarks != null ? maxMarks : 100);
        assignment.setSubject(subject);

        if (files != null) {
            assignment.clearAttachments();
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    assignment.addAttachment(toAssignmentAttachment(file));
                }
            }
        }
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public void delete(Long id, User requestingUser) {
        Assignment assignment = getById(id);

        if (requestingUser.getRole() == Role.FACULTY || requestingUser.getRole() == Role.HOD) {
            if (!assignment.getFaculty().getId().equals(requestingUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete assignments you created.");
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
            String submissionText,
            MultipartFile[] files) {

        Assignment assignment = getById(assignmentId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (student.getRole() != Role.STUDENT) {
            throw new RuntimeException("Only students can submit assignments.");
        }

        boolean hasText = submissionText != null && !submissionText.trim().isEmpty();
        boolean hasFiles = files != null && java.util.Arrays.stream(files).anyMatch(file -> file != null && !file.isEmpty());
        if (!hasText && !hasFiles) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide an answer, upload at least one file, or do both.");
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
        submission.setSubmissionText(hasText ? submissionText.trim() : "");
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus(status);

        if (hasFiles) {
            submission.getAttachments().clear();
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    submission.addAttachment(toSubmissionAttachment(file));
                }
            }
        }

        return submissionRepository.save(submission);
    }

    public AssignmentSubmission submitAssignment(
            Long assignmentId,
            Long studentId,
            String submissionText) {
        return submitAssignment(assignmentId, studentId, submissionText, null);
    }

    public List<AttachmentMetadata> getAssignmentAttachmentMetadata(Long assignmentId) {
        getById(assignmentId);
        return assignmentAttachmentRepository.findMetadataByAssignmentId(assignmentId);
    }

    @Transactional(readOnly = true)
    public AttachmentFile getAssignmentAttachment(Long assignmentId, Long attachmentId) {
        AssignmentAttachment attachment = assignmentAttachmentRepository
                .findByIdAndAssignment_Id(attachmentId, assignmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
        return new AttachmentFile(attachment.getFileName(), attachment.getContentType(), attachment.getData());
    }

    @Transactional(readOnly = true)
    public List<AttachmentMetadata> getSubmissionAttachmentMetadata(
            Long submissionId,
            User requestingUser) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
        validateSubmissionAccess(submission, requestingUser);
        return submissionAttachmentRepository.findMetadataBySubmissionId(submissionId);
    }

    @Transactional(readOnly = true)
    public AttachmentFile getSubmissionAttachment(
            Long submissionId,
            Long attachmentId,
            User requestingUser) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
        validateSubmissionAccess(submission, requestingUser);
        SubmissionAttachment attachment = submissionAttachmentRepository
                .findByIdAndSubmission_Id(attachmentId, submissionId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
        return new AttachmentFile(attachment.getFileName(), attachment.getContentType(), attachment.getData());
    }

    private void validateSubmissionAccess(AssignmentSubmission submission, User requestingUser) {
        if (requestingUser.getRole() == Role.STUDENT
                && !submission.getStudent().getId().equals(requestingUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own submission.");
        }
        if (requestingUser.getRole() == Role.FACULTY
                && !isFacultyAssignedToSubject(submission.getAssignment().getSubject(), requestingUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access submissions for your assignments.");
        }
    }

    private AssignmentAttachment toAssignmentAttachment(MultipartFile file) {
        AssignmentAttachment attachment = new AssignmentAttachment();
        attachment.setFileName(file.getOriginalFilename());
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        try {
            attachment.setData(file.getBytes());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded file.", exception);
        }
        return attachment;
    }

    private SubmissionAttachment toSubmissionAttachment(MultipartFile file) {
        SubmissionAttachment attachment = new SubmissionAttachment();
        attachment.setFileName(file.getOriginalFilename());
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        try {
            attachment.setData(file.getBytes());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read uploaded file.", exception);
        }
        return attachment;
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
