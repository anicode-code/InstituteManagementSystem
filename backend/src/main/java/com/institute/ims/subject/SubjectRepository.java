package com.institute.ims.subject;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository
        extends JpaRepository<Subject, Long> {

    List<Subject> findByFacultyId(Long facultyId);

    List<Subject> findByDepartmentId(Long departmentId);
}