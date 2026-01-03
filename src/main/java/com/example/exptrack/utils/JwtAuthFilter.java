
package com.example.exptrack.utils;

import java.io.IOException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.exptrack.dtos.UserDTO;
import com.example.exptrack.services.CookieService;
import com.example.exptrack.services.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  @Autowired
  private JwtService jwtService;

  @Autowired
  private CookiesExtractor cookiesExtractor;

  @Autowired
  private CookieService cookieService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String path = request.getServletPath();

    // ✅ Public endpoints
    if (path.startsWith("/api/public")
        || (path.startsWith("/auth") && !path.equals("/auth/me") && !path.equals("/auth/logout"))) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      // 1️⃣ Try access token
      String accessToken = cookiesExtractor.extractCookie(request, "access_token");

      if (accessToken != null
          && jwtService.isAccessToken(accessToken)
          && !jwtService.isTokenExpired(accessToken)) {

        authenticate(accessToken);
        filterChain.doFilter(request, response);
        return;
      }

      // 2️⃣ Access expired → try refresh token
      String refreshToken = cookiesExtractor.extractCookie(request, "refresh_token");

      if (refreshToken != null
          && jwtService.isRefreshToken(refreshToken)
          && !jwtService.isTokenExpired(refreshToken)) {

        JwtService.TokenPair tokens = jwtService.refreshTokenPair(refreshToken);

        // 🔄 Rotate cookies
        cookieService.addCookie(
            response,
            "access_token",
            tokens.getAccessToken(),
            jwtService.getMillisUntilExpiration(tokens.getAccessToken()) / 1000);

        cookieService.addCookie(
            response,
            "refresh_token",
            tokens.getRefreshToken(),
            jwtService.getMillisUntilExpiration(tokens.getRefreshToken()) / 1000);

        authenticate(tokens.getAccessToken());
        filterChain.doFilter(request, response);
        return;
      }

      // ❌ No valid tokens
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;

    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
  }

  private void authenticate(String token) {
    UserDTO user = jwtService.extractUserDTO(token);

    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        user,
        null,
        Collections.emptyList());

    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
