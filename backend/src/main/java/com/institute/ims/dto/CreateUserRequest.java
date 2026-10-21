package com.institute.ims.dto;

import com.institute.ims.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateUserRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String rollNumber;

    @NotBlank
    private String password;

    @Email
    private String email;

    @NotNull
    private Role role;

    private Long departmentId;

    public String getName() {
        return name;
    }

    public String getRollNumber() {
        return rollNumber != null ? rollNumber.trim() : null;
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

    public Long getDepartmentId() {
        return departmentId;
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

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }
}
