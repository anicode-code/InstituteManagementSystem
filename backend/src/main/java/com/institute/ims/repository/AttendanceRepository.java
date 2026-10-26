package com.institute.ims.repository;

import com.institute.ims.model.Attendance;
import com.institute.ims.model.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository
        extends JpaRepository<Attendance, Long> {

    List<Attendance> findByStudentId(Long studentId);

    List<Attendance> findBySubjectId(Long subjectId);

    List<Attendance> findByStudentIdAndSubjectId(
            Long studentId,
            Long subjectId
    );

    Optional<Attendance> findByStudentIdAndSubjectIdAndDate(
            Long studentId,
            Long subjectId,
            LocalDate date
    );

    List<Attendance> findBySubjectIdAndDate(
            Long subjectId,
            LocalDate date
    );

    @Query("SELECT a FROM Attendance a JOIN a.subject.facultyMembers f WHERE f.id = :facultyId")
    List<Attendance> findBySubjectFacultyId(@Param("facultyId") Long facultyId);

    List<Attendance> findBySubjectDepartmentId(Long departmentId);

    long countByStudentIdAndStatus(Long studentId, AttendanceStatus status);

    long countByStudentId(Long studentId);

    @Transactional
    void deleteBySubjectIdAndDate(Long subjectId, LocalDate date);
}
