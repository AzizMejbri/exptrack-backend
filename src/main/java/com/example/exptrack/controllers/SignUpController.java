
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/auth/signup")
public class SignUpController {

  @Autowired
  private UserService userService;

  @PostMapping
  @Operation(summary = "Register a new user", description = """
      Creates a new user account in the system.

      **Request Body:**
      - `email` (string): user's email, must be unique
      - `username` (string): desired username
      - `password` (string): account password

      **Response:**
      - 200: User created successfully, returns created user and message
      - 417: Signup failed, returns error message

      **Public Endpoint:** No authentication required
      """, security = {} // explicitly public endpoint
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User created successfully", content = @Content(mediaType = "application/json", schema = @Schema(example = """
          {
            "message": "user created",
            "user": {
              "id": 1,
              "email": "user@example.com",
              "username": "myusername"
            }
          }
          """))),
      @ApiResponse(responseCode = "417", description = "Signup failed", content = @Content(mediaType = "application/json", schema = @Schema(example = """
          {
            "message": "Email already exists"
          }
          """)))
  })
  public ResponseEntity<?> handleSignup(@RequestBody SignupRequest signupRequest) {
    try {
      User createdUser = userService.saveUser(
          new User(signupRequest.email(), signupRequest.username(), signupRequest.password()));

      return ResponseEntity.ok(Map.of(
          "message", "user created",
          "user", createdUser));
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(Map.of("message", e.getMessage()));
    }
  }
}
