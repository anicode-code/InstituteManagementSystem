package com.institute.ims.dto;

import com.institute.ims.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Roll number is required")
    private String rollNumber;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    private String email;

    @NotNull(message = "Role is required")
    private Role role;

    // Single department (used by students)
    private Long departmentId;

    // Multiple departments (used by faculty)
    private List<Long> departmentIds;

    // Student academic fields
    private Integer year;
    private Integer semester;

    // Subject codes for optional subjects selected by the student
    private List<String> optionalSubjectCodes;

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

    public List<Long> getDepartmentIds() {
        return departmentIds;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getSemester() {
        return semester;
    }

    public List<String> getOptionalSubjectCodes() {
        return optionalSubjectCodes;
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

    public void setDepartmentIds(List<Long> departmentIds) {
        this.departmentIds = departmentIds;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public void setOptionalSubjectCodes(List<String> optionalSubjectCodes) {
        this.optionalSubjectCodes = optionalSubjectCodes;
    }
}
