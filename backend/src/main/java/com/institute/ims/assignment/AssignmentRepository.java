package com.institute.ims.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository
        extends JpaRepository<Assignment, Long> {

    List<Assignment> findByFacultyId(Long facultyId);

    List<Assignment> findBySubjectId(Long subjectId);
}