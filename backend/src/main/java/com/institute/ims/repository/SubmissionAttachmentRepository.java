package com.institute.ims.repository;

import com.institute.ims.dto.AttachmentMetadata;
import com.institute.ims.model.SubmissionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubmissionAttachmentRepository extends JpaRepository<SubmissionAttachment, Long> {

    @Query("""
            select new com.institute.ims.dto.AttachmentMetadata(a.id, a.fileName, a.contentType, a.fileSize)
            from SubmissionAttachment a
            where a.submission.id = :submissionId
            """)
    List<AttachmentMetadata> findMetadataBySubmissionId(@Param("submissionId") Long submissionId);

    Optional<SubmissionAttachment> findByIdAndSubmission_Id(Long id, Long submissionId);
}
