package com.example.exptrack.services;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CookieService {

  public void deleteCookie(HttpServletResponse response, String name) {
    Cookie cookie = new Cookie(name, "");
    cookie.setHttpOnly(true);
    cookie.setSecure(true); // keep same as login
    cookie.setPath("/"); // MUST match original path
    cookie.setMaxAge(0); // ← kills cookie immediately

    response.addCookie(cookie);
  }

  public void addCookie(HttpServletResponse response,
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

  public String extractCookie(HttpServletRequest request, String name) {
    if (request.getCookies() == null)
      return null;
    for (Cookie cookie : request.getCookies()) {
      if (cookie.getName().equals(name)) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
