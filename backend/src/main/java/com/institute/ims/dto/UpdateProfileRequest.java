package com.institute.ims.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class UpdateProfileRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @Email(message = "Invalid email")
    private String email;

    private Integer year;
    private Integer semester;
    private List<String> optionalSubjectCodes;

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
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

    public void setEmail(String email) {
        this.email = email != null && email.isBlank() ? null : email;
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
