package com.institute.ims.dto;

public class LoginResponse {

    private String token;
    private Long id;
    private String name;
    private String rollNumber;
    private String role;
    private String status;

    public LoginResponse(
            String token,
            Long id,
            String name,
            String rollNumber,
            String role,
            String status) {

        this.token = token;
        this.id = id;
        this.name = name;
        this.rollNumber = rollNumber;
        this.role = role;
        this.status = status;
    }

    public String getToken() {
        return token;
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

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }
}
