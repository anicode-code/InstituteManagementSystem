package com.institute.ims.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "faculty_profile_audits")
public class FacultyProfileAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "faculty_user_id", nullable = false)
    private Long facultyUserId;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "previous_value", columnDefinition = "TEXT")
    private String previousValue;

    @Column(name = "updated_value", columnDefinition = "TEXT")
    private String updatedValue;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    protected FacultyProfileAudit() {
    }

    public FacultyProfileAudit(
            Long facultyUserId,
            String changedBy,
            String fieldName,
            String previousValue,
            String updatedValue,
            LocalDateTime changedAt) {
        this.facultyUserId = facultyUserId;
        this.changedBy = changedBy;
        this.fieldName = fieldName;
        this.previousValue = previousValue;
        this.updatedValue = updatedValue;
        this.changedAt = changedAt;
    }

    public Long getFacultyUserId() {
        return facultyUserId;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getPreviousValue() {
        return previousValue;
    }

    public String getUpdatedValue() {
        return updatedValue;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
