package com.example.exptrack.services;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.exptrack.dtos.UserDTO;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

@Service
public class JwtService {

  private final SecretKey SECRET_KEY;
  private final Long ACCESS_TOKEN_EXPIRATION;
  private final Long REFRESH_TOKEN_EXPIRATION;

  public JwtService(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.expiration}") Long accessExpiration,
      @Value("${jwt.refresh-expiration}") Long refreshExpiration) {

    // Convert the string secret to a secure key
    this.SECRET_KEY = Keys.hmacShaKeyFor(secret.getBytes());
    this.ACCESS_TOKEN_EXPIRATION = accessExpiration;
    this.REFRESH_TOKEN_EXPIRATION = refreshExpiration;
  }

  public String generateAccessToken(UserDTO user) {
    return generateToken(user, ACCESS_TOKEN_EXPIRATION, "ACCESS");
  }

  public String generateRefreshToken(UserDTO user) {
    return generateToken(user, REFRESH_TOKEN_EXPIRATION, "REFRESH");
  }

  public TokenPair generateTokenPair(UserDTO user) {
    return new TokenPair(
        generateAccessToken(user),
        generateRefreshToken(user));
  }

  private String generateToken(UserDTO user, Long expiration, String tokenType) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("id", user.id());
    claims.put("username", user.username());
    claims.put("id", user.id()); // Store serialized UserDTO
    claims.put("tokenType", tokenType); // Distinguish between token types

    return Jwts.builder()
        .setClaims(claims)
        .setSubject(user.username())
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
        .compact();
  }

  public Claims extractAllClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(SECRET_KEY)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  public UserDTO extractUserDTO(String token) {
    Claims claims = extractAllClaims(token);

    Long id = claims.get("id", Long.class);
    String username = claims.get("username", String.class);

    return new UserDTO(id, username);
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  public String extractTokenType(String token) {
    return extractClaim(token, claims -> claims.get("tokenType", String.class));
  }

  public boolean isTokenValid(String token, UserDTO user) {
    final String username = extractUsername(token);
    return (username.equals(user.username()) && !isTokenExpired(token));
  }

  public boolean isAccessToken(String token) {
    try {
      return "ACCESS".equals(extractTokenType(token));
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isRefreshToken(String token) {
    try {
      return "REFRESH".equals(extractTokenType(token));
    } catch (Exception e) {
      return false;
    }
  }

  public boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  public boolean canTokenBeRefreshed(String accessToken, String refreshToken) {
    try {
      // Validate that refresh token is actually a refresh token
      if (!isRefreshToken(refreshToken)) {
        return false;
      }

      // Check if refresh token is expired
      if (isTokenExpired(refreshToken)) {
        return false;
      }

      // Extract user from both tokens
      UserDTO userFromAccessToken = extractUserDTO(accessToken);
      UserDTO userFromRefreshToken = extractUserDTO(refreshToken);

      // Verify both tokens belong to the same user
      return userFromAccessToken.id().equals(userFromRefreshToken.id()) &&
          userFromAccessToken.username().equals(userFromRefreshToken.username());

    } catch (Exception e) {
      return false;
    }
  }

  public String refreshAccessToken(String refreshToken) {
    if (!isRefreshToken(refreshToken)) {
      throw new IllegalArgumentException("Invalid refresh token type");
    }

    if (isTokenExpired(refreshToken)) {
      throw new IllegalArgumentException("Refresh token has expired");
    }

    UserDTO user = extractUserDTO(refreshToken);
    return generateAccessToken(user);
  }

  public TokenPair refreshTokenPair(String refreshToken) {
    if (!isRefreshToken(refreshToken)) {
      throw new IllegalArgumentException("Invalid refresh token type");
    }

    if (isTokenExpired(refreshToken)) {
      throw new IllegalArgumentException("Refresh token has expired");
    }

    UserDTO user = extractUserDTO(refreshToken);
    return generateTokenPair(user);
  }

  // Helper class for returning token pairs
  public static class TokenPair {
    private final String accessToken;
    private final String refreshToken;

    public TokenPair(String accessToken, String refreshToken) {
      this.accessToken = accessToken;
      this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
      return accessToken;
    }

    public String getRefreshToken() {
      return refreshToken;
    }

    // Helper method to create a map response
    public Map<String, String> toMap() {
      Map<String, String> map = new HashMap<>();
      map.put("accessToken", accessToken);
      map.put("refreshToken", refreshToken);
      return map;
    }
  }

  // Utility method to get time until expiration
  public long getMillisUntilExpiration(String token) {
    Date expiration = extractExpiration(token);
    return expiration.getTime() - System.currentTimeMillis();
  }

  // Check if token should be refreshed (e.g., expires in less than 5 minutes)
  public boolean shouldRefreshAccessToken(String accessToken) {
    long timeUntilExpiration = getMillisUntilExpiration(accessToken);
    long refreshThreshold = 5 * 60 * 1000; // 5 minutes in milliseconds

    return timeUntilExpiration > 0 && timeUntilExpiration < refreshThreshold;
  }
}
