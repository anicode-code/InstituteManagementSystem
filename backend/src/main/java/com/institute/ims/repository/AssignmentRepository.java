package com.institute.ims.repository;

import com.institute.ims.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssignmentRepository
        extends JpaRepository<Assignment, Long> {

    List<Assignment> findByFacultyId(Long facultyId);

    @Query("SELECT DISTINCT a FROM Assignment a JOIN a.subject.facultyMembers f WHERE f.id = :facultyId")
    List<Assignment> findBySubjectFacultyId(@Param("facultyId") Long facultyId);

    List<Assignment> findBySubjectId(Long subjectId);

    List<Assignment> findBySubjectIdIn(List<Long> subjectIds);

    List<Assignment> findBySubjectDepartmentId(Long departmentId);
}
