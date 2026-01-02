package com.example.exptrack.controllers;

import java.nio.file.AccessDeniedException;
import java.util.Collections;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<?> handleAccessDenied(AccessDeniedException e) {
    // 403 is more appropriate for authorization failures
    return ResponseEntity.status(403)
        .body(Collections.singletonMap("error", "Access denied"));
  }

  @ExceptionHandler(NullPointerException.class)
  public ResponseEntity<?> handleNullPointer(NullPointerException e) {
    // Log the error for debugging
    System.err.println("\u001B[31mNullPointerException: " + e + "\u001B[0m");
    // Return 500 for programming errors
    return ResponseEntity.status(500)
        .body(Collections.singletonMap("error", "Internal server error"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleException(Exception e) {
    // Return 500 for unexpected errors
    return ResponseEntity.status(500)
        .body(Collections.singletonMap("error", "Internal server error"));
  }
}
