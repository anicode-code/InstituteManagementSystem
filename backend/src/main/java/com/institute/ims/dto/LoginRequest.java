package com.institute.ims.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Roll number is required")
    private String rollNumber;

    @NotBlank(message = "Password is required")
    private String password;

    public String getRollNumber() {
        return rollNumber != null ? rollNumber.trim() : null;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
