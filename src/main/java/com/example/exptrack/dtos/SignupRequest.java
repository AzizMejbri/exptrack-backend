package com.example.exptrack.dtos;

public record SignupRequest(
    String username,
    String email,
    String password) {
}
