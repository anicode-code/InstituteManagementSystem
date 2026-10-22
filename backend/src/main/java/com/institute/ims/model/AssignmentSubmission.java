package com.institute.ims.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

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
