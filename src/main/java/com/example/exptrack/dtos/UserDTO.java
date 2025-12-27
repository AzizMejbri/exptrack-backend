package com.example.exptrack.dtos;

public record UserDTO(Long id, String username) {
  public String serialize() {
    return "{id: " + id + ", username: " + username + "}";
  }
}
