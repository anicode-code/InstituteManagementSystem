package com.institute.ims.subject;

import com.institute.ims.department.Department;
import com.institute.ims.user.User;
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

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne
    @JoinColumn(name = "faculty_id")
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
}