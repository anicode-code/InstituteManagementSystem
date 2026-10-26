package com.institute.ims.repository;

import com.institute.ims.model.Subject;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository
        extends JpaRepository<Subject, Long> {

    @Query("SELECT DISTINCT s FROM Subject s JOIN s.facultyMembers f WHERE f.id = :facultyId")
    List<Subject> findByFacultyId(@Param("facultyId") Long facultyId);

    List<Subject> findByDepartmentId(Long departmentId);

    List<Subject> findByDepartmentIdIn(List<Long> departmentIds);

    List<Subject> findByDepartmentIdAndSemester(Long departmentId, Integer semester);

    Optional<Subject> findByCode(String code);

    boolean existsByCode(String code);

    List<Subject> findByCodeIn(List<String> codes);
}
