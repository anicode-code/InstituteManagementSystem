package com.institute.ims.repository;

import com.institute.ims.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository
        extends JpaRepository<Subject, Long> {

    List<Subject> findByFacultyId(Long facultyId);

    List<Subject> findByDepartmentId(Long departmentId);

    List<Subject> findByDepartmentIdAndSemester(Long departmentId, Integer semester);

    Optional<Subject> findByCode(String code);

    boolean existsByCode(String code);

    List<Subject> findByCodeIn(List<String> codes);
}

