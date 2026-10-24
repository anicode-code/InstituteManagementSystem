package com.institute.ims.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "roll_number", nullable = false, unique = true)
    private String rollNumber;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Primary department (backward-compatible, used by students)
    @ManyToOne
    @JoinColumn(name = "department_id")
    @JsonIgnoreProperties({"hod"})
    private Department department;

    // Multiple departments (used by faculty)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_departments",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    @JsonIgnoreProperties({"hod"})
    private Set<Department> departments = new HashSet<>();

    // Student academic fields
    @Column(name = "academic_year")
    private Integer year;

    @Column(name = "semester")
    private Integer semester;

    // Optional subjects selected by a student
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "student_selected_subjects",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    @JsonIgnoreProperties({"faculty", "department"})
    private Set<Subject> selectedSubjects = new HashSet<>();

    public User() {
    }

    public User(String name, String rollNumber, String password,
                String email, Role role, Department department) {
        this.name = name;
        this.rollNumber = rollNumber;
        this.password = password;
        this.email = email;
        this.role = role;
        this.department = department;
        this.status = VerificationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public User(String name, String rollNumber, String password,
                String email, Role role, Department department, VerificationStatus status) {
        this.name = name;
        this.rollNumber = rollNumber;
        this.password = password;
        this.email = email;
        this.role = role;
        this.department = department;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Department getDepartment() {
        return department;
    }

    public Set<Department> getDepartments() {
        return departments;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getSemester() {
        return semester;
    }

    public Set<Subject> getSelectedSubjects() {
        return selectedSubjects;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public void setDepartments(Set<Department> departments) {
        this.departments = departments;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public void setSelectedSubjects(Set<Subject> selectedSubjects) {
        this.selectedSubjects = selectedSubjects;
    }
}
