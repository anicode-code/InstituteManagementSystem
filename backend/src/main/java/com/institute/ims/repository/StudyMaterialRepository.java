package com.institute.ims.repository;

import com.institute.ims.dto.StudyMaterialResponse;
import com.institute.ims.model.StudyMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {

    @Query("""
            select new com.institute.ims.dto.StudyMaterialResponse(
                material.id, material.title, material.description, material.fileName,
                material.contentType, material.fileSize, subject.id, subject.name, subject.code,
                uploader.name, material.uploadedAt)
            from StudyMaterial material
            join material.subject subject
            join material.uploadedBy uploader
            where subject.id in :subjectIds
            order by material.uploadedAt desc, material.id desc
            """)
    List<StudyMaterialResponse> findMaterialResponsesBySubjectIds(
            @Param("subjectIds") List<Long> subjectIds);
}
