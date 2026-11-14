package com.institute.ims.repository;

import com.institute.ims.model.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionRepository
        extends JpaRepository<AssignmentSubmission, Long> {

    List<AssignmentSubmission> findByAssignmentId(Long assignmentId);

    List<AssignmentSubmission> findByAssignmentIdIn(List<Long> assignmentIds);

    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    List<AssignmentSubmission> findByStudentId(Long studentId);

    List<AssignmentSubmission> findByStudentIdIn(List<Long> studentIds);

    long countByAssignmentId(Long assignmentId);
}
