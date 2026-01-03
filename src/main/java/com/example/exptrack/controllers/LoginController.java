
package com.example.exptrack.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.example.exptrack.dtos.LoginRequest;
import com.example.exptrack.dtos.UserDTO;
import com.example.exptrack.security.UserDetailsImpl;
import com.example.exptrack.services.CookieService;
import com.example.exptrack.services.JwtService;
// import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class LoginController {

  @Autowired
  private JwtService jwtService;

  @Autowired
  private AuthenticationManager authMan;

  @Autowired
  private CookieService cookieService;

  /* ===================== LOGIN ===================== */
  @PostMapping("/login")
  @Operation(summary = "Login user", description = """
      Authenticates a user using email and password.
      On success:
      - Sets `access_token` and `refresh_token` as HttpOnly cookies
      - Returns JSON containing user id and username
      """, security = {} // public endpoint
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Login successful, tokens set in cookies", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
      @ApiResponse(responseCode = "401", description = "Invalid email or password", content = @Content(mediaType = "application/json"))
  })
  public ResponseEntity<?> handleLogin(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
    try {
      System.out.println("🔍 Login attempt: " + loginRequest.getEmail());
      System.out.println("🔍 Password length: " + loginRequest.getPassword().length());

      Authentication auth = authMan.authenticate(
          new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
      System.out.println("✅ Authentication successful!");
      UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
      JwtService.TokenPair jwtTokens = jwtService.generateTokenPair(new UserDTO(user.getId(), user.getEmail()));

      cookieService.addCookie(response, "access_token",
          jwtTokens.getAccessToken(),
          jwtService.getMillisUntilExpiration(jwtTokens.getAccessToken()) / 1000);

      cookieService.addCookie(response, "refresh_token",
          jwtTokens.getRefreshToken(),
          jwtService.getMillisUntilExpiration(jwtTokens.getRefreshToken()) / 1000);

      return ResponseEntity.ok(Map.of("id", user.getId(), "username", user.getActualUsername()));
    } catch (Exception e) {
      System.out.println("❌ Authentication failed: " + e.getMessage());
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    }
  }

  /* ===================== REFRESH TOKEN ===================== */
  @PostMapping("/login/refresh")
  @Operation(summary = "Refresh JWT token pair", description = """
      Uses the provided refresh token to generate a new pair of access and refresh tokens.
      Sets the new access token as a HttpOnly cookie.
      """, security = {} // public endpoint
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Tokens refreshed successfully", content = @Content(mediaType = "application/json")),
      @ApiResponse(responseCode = "401", description = "Invalid refresh token", content = @Content(mediaType = "application/json"))
  })
  public ResponseEntity<?> refresh(@RequestBody Map<String, String> request, HttpServletResponse response) {
    String refreshToken = request.get("refreshToken");
    try {
      JwtService.TokenPair newTokens = jwtService.refreshTokenPair(refreshToken);
      cookieService.addCookie(response, "access_token",
          newTokens.getAccessToken(),
          jwtService.getMillisUntilExpiration(newTokens.getAccessToken()) / 1000);
      return ResponseEntity.ok(newTokens.toMap());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
    }
  }

  /* ===================== REFRESH ACCESS TOKEN ONLY ===================== */
  @PostMapping("/login/refresh-access")
  @Operation(summary = "Refresh access token", description = """
      Uses a refresh token to refresh the access token only.
      Returns the new access token in the response body.
      """, security = {} // public endpoint
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Access token refreshed successfully", content = @Content(mediaType = "application/json")),
      @ApiResponse(responseCode = "401", description = "Cannot refresh token or invalid refresh token", content = @Content(mediaType = "application/json"))
  })
  public ResponseEntity<?> refreshAccessToken(@RequestBody Map<String, String> request) {
    String accessToken = request.get("accessToken");
    String refreshToken = request.get("refreshToken");

    if (jwtService.canTokenBeRefreshed(accessToken, refreshToken)) {
      try {
        String newAccessToken = jwtService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
      } catch (IllegalArgumentException e) {
        return ResponseEntity.status(401).body(Map.of("error", "Failed to refresh token"));
      }
    }
    return ResponseEntity.status(401).body(Map.of("error", "Cannot refresh token"));
  }

  /* ===================== LOGOUT ===================== */
  @PostMapping("/logout")
  @Operation(summary = "Logout user", description = "Deletes the access and refresh tokens from cookies, effectively logging out the user.", security = @SecurityRequirement(name = "cookieAuth"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Logged out successfully", content = @Content(mediaType = "application/json"))
  })
  public ResponseEntity<?> logout(HttpServletResponse response) {
    cookieService.deleteCookie(response, "access_token");
    cookieService.deleteCookie(response, "refresh_token");
    return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
  }

  /* ===================== GET CURRENT USER ===================== */
  @GetMapping("/me")
  @Operation(summary = "Get current authenticated user", description = "Returns the details of the currently authenticated user.", security = @SecurityRequirement(name = "cookieAuth"))
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class))),
      @ApiResponse(responseCode = "401", description = "User not authenticated", content = @Content(mediaType = "application/json"))
  })
  public ResponseEntity<UserDTO> me(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
    UserDTO user = new UserDTO(principal.getId(), principal.getActualUsername());
    return ResponseEntity.ok(user);
  }
}
