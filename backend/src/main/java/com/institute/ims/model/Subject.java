package com.institute.ims.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    private Integer credits;

    private Integer semester;

    @Column(name = "academic_year")
    private Integer academicYear;

    @Enumerated(EnumType.STRING)
    private SubjectType type = SubjectType.COMPULSORY;

    @ManyToOne
    @JoinColumn(name = "department_id")
    @JsonIgnoreProperties({"hod"})
    private Department department;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "subject_faculty",
        joinColumns = @JoinColumn(name = "subject_id"),
        inverseJoinColumns = @JoinColumn(name = "faculty_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"subject_id", "faculty_id"})
    )
    @JsonIgnoreProperties({"password", "departments", "selectedSubjects", "department"})
    private Set<User> facultyMembers = new HashSet<>();

    // Retained read-only to support the one-time backfill from the legacy column.
    @ManyToOne
    @JoinColumn(name = "faculty_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"password", "departments", "selectedSubjects", "department"})
    private User legacyFaculty;

    public Subject() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Integer getCredits() {
        return credits;
    }

    public Department getDepartment() {
        return department;
    }

    public Set<User> getFacultyMembers() {
        return facultyMembers;
    }

    public User getFaculty() {
        if (facultyMembers != null && !facultyMembers.isEmpty()) {
            return facultyMembers.iterator().next();
        }
        return null;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setCredits(Integer credits) {
        this.credits = credits;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public void setFaculty(User faculty) {
        this.facultyMembers.clear();
        if (faculty != null) {
            this.facultyMembers.add(faculty);
        }
    }

    public void setFacultyMembers(Set<User> facultyMembers) {
        this.facultyMembers = facultyMembers != null ? facultyMembers : new HashSet<>();
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public Integer getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(Integer academicYear) {
        this.academicYear = academicYear;
    }

    public SubjectType getType() {
        return type;
    }

    public void setType(SubjectType type) {
        this.type = type;
    }
}
