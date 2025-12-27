package com.example.exptrack.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.exptrack.dtos.SignupRequest;
import com.example.exptrack.models.User;
import com.example.exptrack.services.UserService;

@RestController
@RequestMapping("/auth/signup")
public class SignUpController {

  @Autowired
  private UserService userService;

  @PostMapping
  public ResponseEntity<?> handleSignup(@RequestBody SignupRequest signupRequest) {
    try {
      return ResponseEntity.ok(Map.of("message", "user created",
          "user",
          userService.saveUser(new User(signupRequest.email(), signupRequest.username(), signupRequest.password()))));
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(Map.of("message", e.getMessage()));
    }
  }
}
