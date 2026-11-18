package com.institute.ims.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate dueDate;

    @Column(name = "max_marks")
    private Integer maxMarks = 100;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(optional = false)
    @JoinColumn(name = "faculty_id")
    private User faculty;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssignmentAttachment> attachments = new ArrayList<>();

    public Assignment() {
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Integer getMaxMarks() {
        return maxMarks != null ? maxMarks : 100;
    }

    public Subject getSubject() {
        return subject;
    }

    public User getFaculty() {
        return faculty;
    }

    @JsonIgnore
    public List<AssignmentAttachment> getAttachments() {
        return attachments;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void setMaxMarks(Integer maxMarks) {
        this.maxMarks = maxMarks;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public void setFaculty(User faculty) {
        this.faculty = faculty;
    }

    public void setAttachments(List<AssignmentAttachment> attachments) {
        this.attachments = attachments;
    }

    public void addAttachment(AssignmentAttachment attachment) {
        attachments.add(attachment);
        attachment.setAssignment(this);
    }

    public void clearAttachments() {
        attachments.clear();
    }
}
