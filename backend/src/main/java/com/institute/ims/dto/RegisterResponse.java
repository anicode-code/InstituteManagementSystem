package com.institute.ims.dto;

public class RegisterResponse {

    private Long id;
    private String name;
    private String username;
    private String email;
    private String role;
    private Long departmentId;

    public RegisterResponse(
            Long id,
            String name,
            String username,
            String email,
            String role,
            Long departmentId) {

        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.role = role;
        this.departmentId = departmentId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public Long getDepartmentId() {
        return departmentId;
    }
}
