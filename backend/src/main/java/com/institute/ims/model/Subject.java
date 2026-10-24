package com.institute.ims.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

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

    @ManyToOne
    @JoinColumn(name = "faculty_id")
    @JsonIgnoreProperties({"password", "departments", "selectedSubjects", "department"})
    private User faculty;

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

    public User getFaculty() {
        return faculty;
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
        this.faculty = faculty;
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
