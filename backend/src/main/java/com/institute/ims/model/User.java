package com.institute.ims.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDateTime;

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

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

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
}
