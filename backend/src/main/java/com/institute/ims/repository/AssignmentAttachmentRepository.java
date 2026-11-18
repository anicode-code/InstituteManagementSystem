package com.institute.ims.repository;

import com.institute.ims.dto.AttachmentMetadata;
import com.institute.ims.model.AssignmentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssignmentAttachmentRepository extends JpaRepository<AssignmentAttachment, Long> {

    @Query("""
            select new com.institute.ims.dto.AttachmentMetadata(a.id, a.fileName, a.contentType, a.fileSize)
            from AssignmentAttachment a
            where a.assignment.id = :assignmentId
            """)
    List<AttachmentMetadata> findMetadataByAssignmentId(@Param("assignmentId") Long assignmentId);

    Optional<AssignmentAttachment> findByIdAndAssignment_Id(Long id, Long assignmentId);
}
