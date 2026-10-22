package com.institute.ims.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "attendance",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"student_id", "subject_id", "date"})
        }
)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    @Column(name = "performance_marks")
    private Integer performanceMarks;

    public Attendance() {
    }

    public Long getId() {
        return id;
    }

    public User getStudent() {
        return student;
    }

    public Subject getSubject() {
        return subject;
    }

    public LocalDate getDate() {
        return date;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public Integer getPerformanceMarks() {
        return performanceMarks;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public void setPerformanceMarks(Integer performanceMarks) {
        this.performanceMarks = performanceMarks;
    }
}
