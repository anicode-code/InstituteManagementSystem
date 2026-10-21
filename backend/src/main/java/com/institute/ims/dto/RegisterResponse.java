package com.institute.ims.dto;

public class RegisterResponse {

    private Long id;
    private String name;
    private String rollNumber;
    private String email;
    private String role;
    private String status;
    private Long departmentId;

    public RegisterResponse(
            Long id,
            String name,
            String rollNumber,
            String email,
            String role,
            String status,
            Long departmentId) {

        this.id = id;
        this.name = name;
        this.rollNumber = rollNumber;
        this.email = email;
        this.role = role;
        this.status = status;
        this.departmentId = departmentId;
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

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public Long getDepartmentId() {
        return departmentId;
    }
}
