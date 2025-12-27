package com.example.exptrack.utils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.exptrack.dtos.UserDTO;
import com.example.exptrack.services.JwtService;
import com.example.exptrack.services.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  @Autowired
  private JwtService jwtService;

  @Autowired
  private UserService userService;

  @Autowired
  private CookiesExtractor cookiesExtractor;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String path = request.getServletPath();

    // Public endpoints
    if (path.startsWith("/api/public") || path.startsWith("/auth")) {
      filterChain.doFilter(request, response);
      return;
    }

    // Extract access token from cookie
    String token = cookiesExtractor.extractCookie(request, "access_token");

    if (token != null && jwtService.isAccessToken(token) && !jwtService.isTokenExpired(token)) {
      try {
        // Extract user from JWT
        var userDTO = jwtService.extractUserDTO(token);

        // Create authentication object
        var authentication = new UsernamePasswordAuthenticationToken(
            userDTO,
            null,
            Collections.emptyList());

        // Set authentication in Spring Security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

      } catch (Exception e) {
        logger.error("Cannot set user authentication: " + e.getMessage());
      }
    }

    filterChain.doFilter(request, response);
  }

}
