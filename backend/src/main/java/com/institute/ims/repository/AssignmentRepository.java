package com.institute.ims.repository;

import com.institute.ims.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository
        extends JpaRepository<Assignment, Long> {

    List<Assignment> findByFacultyId(Long facultyId);

    List<Assignment> findBySubjectId(Long subjectId);

    List<Assignment> findBySubjectDepartmentId(Long departmentId);
}
