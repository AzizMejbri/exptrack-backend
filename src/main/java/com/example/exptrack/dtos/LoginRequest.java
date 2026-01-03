package com.example.exptrack.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginRequest {

  @JsonProperty("email")
  private String email;
  @JsonProperty("password")
  private String password;

  public LoginRequest(String email, String password) {
    this.email = email;
    this.password = password;
  }

  public LoginRequest() {
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
