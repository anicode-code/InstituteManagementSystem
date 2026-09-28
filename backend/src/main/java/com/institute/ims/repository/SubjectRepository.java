package com.institute.ims.repository;

import com.institute.ims.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository
        extends JpaRepository<Subject, Long> {

    List<Subject> findByFacultyId(Long facultyId);

    List<Subject> findByDepartmentId(Long departmentId);
}
