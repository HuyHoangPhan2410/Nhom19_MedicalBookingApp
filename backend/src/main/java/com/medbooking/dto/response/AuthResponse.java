package com.medbooking.dto.response;

public class AuthResponse {
    private Integer userId;
    private String email;
    private String role;
    private String fullName;
    private String token;

    public AuthResponse() {}

    public AuthResponse(Integer userId, String email, String role, String fullName, String token) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.fullName = fullName;
        this.token = token;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
