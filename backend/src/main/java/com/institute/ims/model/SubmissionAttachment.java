package com.institute.ims.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "submission_attachments")
public class SubmissionAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "submission_id")
    @JsonIgnore
    private AssignmentSubmission submission;

    @Column(nullable = false)
    private String fileName;

    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @JsonIgnore
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] data;

    public Long getId() { return id; }
    public AssignmentSubmission getSubmission() { return submission; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    @JsonIgnore
    public byte[] getData() { return data; }

    public void setId(Long id) { this.id = id; }
    public void setSubmission(AssignmentSubmission submission) { this.submission = submission; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public void setData(byte[] data) { this.data = data; }
}
