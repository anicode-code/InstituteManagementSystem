package com.institute.ims.assignment;

import com.institute.ims.subject.Subject;
import com.institute.ims.subject.SubjectRepository;
import com.institute.ims.user.User;
import com.institute.ims.user.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            SubjectRepository subjectRepository,
            UserRepository userRepository) {

        this.assignmentRepository = assignmentRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
    }

    public List<Assignment> getAll() {
        return assignmentRepository.findAll();
    }

    public Assignment getById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Assignment not found"));
    }

    public List<Assignment> getByFaculty(Long facultyId) {
        return assignmentRepository.findByFacultyId(facultyId);
    }

    public List<Assignment> getBySubject(Long subjectId) {
        return assignmentRepository.findBySubjectId(subjectId);
    }

    public Assignment create(
            String title,
            String description,
            LocalDate dueDate,
            Long subjectId,
            Long facultyId) {

        Subject subject =
                subjectRepository.findById(subjectId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Subject not found"));

        User faculty =
                userRepository.findById(facultyId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Faculty not found"));

        Assignment assignment = new Assignment();

        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setDueDate(dueDate);
        assignment.setSubject(subject);
        assignment.setFaculty(faculty);

        return assignmentRepository.save(assignment);
    }

    public void delete(Long id) {

        if (!assignmentRepository.existsById(id)) {
            throw new RuntimeException(
                    "Assignment not found");
        }

        assignmentRepository.deleteById(id);
    }
}