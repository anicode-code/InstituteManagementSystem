package com.institute.ims.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "assignment_submissions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"assignment_id", "student_id"})
        }
)
public class AssignmentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String submissionText;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubmissionAttachment> attachments = new ArrayList<>();

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    private Integer marks;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    public AssignmentSubmission() {
    }

    public Long getId() {
        return id;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public User getStudent() {
        return student;
    }

    public String getSubmissionText() {
        return submissionText;
    }

    @JsonIgnore
    public List<SubmissionAttachment> getAttachments() {
        return attachments;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public Integer getMarks() {
        return marks;
    }

    public String getFeedback() {
        return feedback;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public void setSubmissionText(String submissionText) {
        this.submissionText = submissionText;
    }

    public void setAttachments(List<SubmissionAttachment> attachments) {
        this.attachments = attachments;
    }

    public void addAttachment(SubmissionAttachment attachment) {
        attachments.add(attachment);
        attachment.setSubmission(this);
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public void setMarks(Integer marks) {
        this.marks = marks;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public void setStatus(SubmissionStatus status) {
        this.status = status;
    }
}
