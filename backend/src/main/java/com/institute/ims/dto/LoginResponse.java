package com.institute.ims.dto;

public class LoginResponse {

    private String token;
    private Long id;
    private String name;
    private String username;
    private String role;

    public LoginResponse(
            String token,
            Long id,
            String name,
            String username,
            String role) {

        this.token = token;
        this.id = id;
        this.name = name;
        this.username = username;
        this.role = role;
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

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}
