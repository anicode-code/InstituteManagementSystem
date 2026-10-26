package com.institute.ims.repository;

import com.institute.ims.model.FacultySubjectRequest;
import com.institute.ims.model.FacultySubjectRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface FacultySubjectRequestRepository extends JpaRepository<FacultySubjectRequest, Long> {

    List<FacultySubjectRequest> findByFacultyIdOrderByCreatedAtDesc(Long facultyId);

    List<FacultySubjectRequest> findByOrderByCreatedAtDesc();

    Optional<FacultySubjectRequest> findFirstByFacultyIdAndSubjectIdAndStatusOrderByCreatedAtDesc(
            Long facultyId,
            Long subjectId,
            FacultySubjectRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM FacultySubjectRequest r WHERE r.id = :requestId")
    Optional<FacultySubjectRequest> findByIdForUpdate(@Param("requestId") Long requestId);
}
