package com.example.exptrack.services;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public class CookieService {

  private String extractCookie(HttpServletRequest request, String name) {
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
