package com.example.ecommerce.dto;

public class AuthResponse {

    private String token;
    private String message;
    private Long userId;

    public AuthResponse() {
    }

    public AuthResponse(
            String token,
            String message,
            Long userId) {

        this.token = token;
        this.message = message;
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}

