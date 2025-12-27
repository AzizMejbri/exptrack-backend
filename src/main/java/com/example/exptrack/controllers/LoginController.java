package com.example.exptrack.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.exptrack.dtos.LoginRequest;
import com.example.exptrack.dtos.UserDTO;
import com.example.exptrack.security.UserDetailsImpl;
import com.example.exptrack.services.JwtService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth/login")
public class LoginController {

  @Autowired
  private JwtService jwtService;
  @Autowired
  private AuthenticationManager authMan;

  private void addCookie(HttpServletResponse response,
      String name,
      String value,
      long maxAgeSeconds) {

    ResponseCookie cookie = ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(true) // required for SameSite=None
        .sameSite("None")
        .path("/")
        .maxAge(maxAgeSeconds)
        .build();

    response.addHeader("Set-Cookie", cookie.toString());
  }

  @PostMapping
  public ResponseEntity<?> handleLogin(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
    try {

      Authentication auth = authMan.authenticate(
          new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
      UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
      JwtService.TokenPair jwtTokens = jwtService.generateTokenPair(new UserDTO(user.getId(), user.getEmail()));

      addCookie(response, "access_token",
          jwtTokens.getAccessToken(),
          jwtService.getMillisUntilExpiration(jwtTokens.getAccessToken()) / 1000);

      addCookie(response, "refresh_token",
          jwtTokens.getRefreshToken(),
          jwtService.getMillisUntilExpiration(jwtTokens.getRefreshToken()) / 1000);
      return ResponseEntity.ok(Map.of("id", user.getId(), "username", user.getUsername()));

    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(@RequestBody Map<String, String> request, HttpServletResponse response) {
    String refreshToken = request.get("refreshToken");

    try {
      JwtService.TokenPair newTokens = jwtService.refreshTokenPair(refreshToken);

      addCookie(response, "access_token",
          newTokens.getAccessToken(),
          jwtService.getMillisUntilExpiration(newTokens.getAccessToken()) / 1000);
      return ResponseEntity.ok(newTokens.toMap());
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(401).body(
          Map.of("error", "Invalid refresh token"));
    }
  }

  @PostMapping("/refresh-access")
  public ResponseEntity<?> refreshAccessToken(@RequestBody Map<String, String> request) {
    String accessToken = request.get("accessToken");
    String refreshToken = request.get("refreshToken");

    if (jwtService.canTokenBeRefreshed(accessToken, refreshToken)) {
      try {
        String newAccessToken = jwtService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
      } catch (IllegalArgumentException e) {
        return ResponseEntity.status(401).body(
            Map.of("error", "Failed to refresh token"));
      }
    }

    return ResponseEntity.status(401).body(
        Map.of("error", "Cannot refresh token"));
  }
}
