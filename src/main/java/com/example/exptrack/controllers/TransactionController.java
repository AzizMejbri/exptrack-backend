
package com.example.exptrack.controllers;

import com.example.exptrack.dtos.*;
import com.example.exptrack.services.JwtService;
import com.example.exptrack.services.TransactionService;
import com.example.exptrack.utils.CookiesExtractor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users/{userId}")
public class TransactionController {

  @Autowired
  private TransactionService transactionService;

  @Autowired
  private CookiesExtractor cookiesExtractor;

  @Autowired
  private JwtService jwtService;

  /* ===================== TRANSACTIONS ===================== */

  @GetMapping("/transactions")
  public ResponseEntity<List<TransactionDTO>> getTransactions(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "month") String timeFrame,
      @RequestParam(defaultValue = "10") int limit,
      @RequestParam(defaultValue = "1") int page,
      HttpServletRequest request,
      HttpServletResponse response) {

    verifyUserAccess(userId, request, response);
    return ResponseEntity.ok(
        transactionService.getTransactions(userId, timeFrame, limit, page));
  }

  @GetMapping("/transactions/recent")
  public ResponseEntity<List<TransactionDTO>> getRecentTransactions(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "5") int limit,
      HttpServletRequest request,
      HttpServletResponse response) {

    verifyUserAccess(userId, request, response);
    return ResponseEntity.ok(
        transactionService.getRecentTransactions(userId, limit));
  }

  @GetMapping("/transactions/summary")
  public ResponseEntity<TransactionSummaryDTO> getTransactionSummary(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "month") String timeFrame,
      HttpServletRequest request,
      HttpServletResponse response) {

    verifyUserAccess(userId, request, response);
    return ResponseEntity.ok(
        transactionService.getTransactionSummary(userId, timeFrame));
  }

  @GetMapping("/transactions/categories/summary")
  public ResponseEntity<List<CategorySummaryDTO>> getCategorySummary(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "month") String timeFrame,
      HttpServletRequest request,
      HttpServletResponse response) {

    verifyUserAccess(userId, request, response);
    return ResponseEntity.ok(
        transactionService.getCategorySummary(userId, timeFrame));
  }

  @GetMapping("/transactions/categories/stats")
  public ResponseEntity<Map<String, Object>> getCategoryStats(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "month") String timeFrame,
      HttpServletRequest request,
      HttpServletResponse response) {

    verifyUserAccess(userId, request, response);
    return ResponseEntity.ok(
        transactionService.getCategoryStats(userId, timeFrame));
  }

  @PostMapping("/transactions")
  public ResponseEntity<TransactionDTO> addTransaction(
      @PathVariable Long userId,
      @RequestBody TransactionDTO transactionDTO,
      HttpServletRequest request,
      HttpServletResponse response) {

    verifyUserAccess(userId, request, response);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(transactionService.addTransaction(userId, transactionDTO));
  }

  @PutMapping("/transactions/{transactionId}")
  public ResponseEntity<TransactionDTO> updateTransaction(
      @PathVariable Long userId,
      @PathVariable Long transactionId,
      @RequestParam String type,
      @RequestBody TransactionDTO transactionDTO,
      HttpServletRequest request,
      HttpServletResponse response) {

    verifyUserAccess(userId, request, response);
    return ResponseEntity.ok(
        transactionService.updateTransaction(transactionId, type, transactionDTO));
  }

  @DeleteMapping("/transactions/{transactionId}")
  public ResponseEntity<Void> deleteTransaction(
      @PathVariable Long userId,
      @PathVariable Long transactionId,
      @RequestParam String type,
      HttpServletRequest request,
      HttpServletResponse response) {

    verifyUserAccess(userId, request, response);
    transactionService.deleteTransaction(transactionId, type);
    return ResponseEntity.noContent().build();
  }

  /* ===================== EXPENSES / REVENUES ===================== */

  @GetMapping("/expenses")
  public ResponseEntity<List<TransactionDTO>> getExpenses(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "month") String timeFrame,
      @RequestParam(defaultValue = "10") int limit,
      @RequestParam(defaultValue = "1") int page,
      HttpServletRequest request,
      HttpServletResponse response) {

    verifyUserAccess(userId, request, response);

    List<TransactionDTO> expenses = transactionService
        .getTransactions(userId, timeFrame, limit * 2, page)
        .stream()
        .filter(t -> "expense".equals(t.getType()))
        .limit(limit)
        .collect(Collectors.toList());

    return ResponseEntity.ok(expenses);
  }

  @GetMapping("/revenues")
  public ResponseEntity<List<TransactionDTO>> getRevenues(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "month") String timeFrame,
      @RequestParam(defaultValue = "10") int limit,
      @RequestParam(defaultValue = "1") int page,
      HttpServletRequest request,
      HttpServletResponse response) {

    verifyUserAccess(userId, request, response);

    List<TransactionDTO> revenues = transactionService
        .getTransactions(userId, timeFrame, limit * 2, page)
        .stream()
        .filter(t -> "revenue".equals(t.getType()))
        .limit(limit)
        .collect(Collectors.toList());

    return ResponseEntity.ok(revenues);
  }

  /* ===================== AUTH HELPERS ===================== */

  private void verifyUserId(UserDTO user, Long requestedUserId) {
    if (!user.id().equals(requestedUserId)) {
      throw new AccessDeniedException(
          "User " + user.id() + " cannot access user " + requestedUserId);
    }
  }

  private void verifyUserAccess(
      Long requestedUserId,
      HttpServletRequest request,
      HttpServletResponse response) {

    try {
      String accessToken = cookiesExtractor.extractCookie(request, "access_token");
      UserDTO authenticatedUser;

      // 1️⃣ Try access token
      if (accessToken != null && !accessToken.isEmpty()
          && jwtService.isAccessToken(accessToken)
          && !jwtService.isTokenExpired(accessToken)) {

        authenticatedUser = jwtService.extractUserDTO(accessToken);
        verifyUserId(authenticatedUser, requestedUserId);
        request.setAttribute("authenticatedUser", authenticatedUser);
        return;
      }

      // 2️⃣ Fallback to refresh token
      String refreshToken = cookiesExtractor.extractCookie(request, "refresh_token");

      if (refreshToken == null || refreshToken.isEmpty()
          || !jwtService.isRefreshToken(refreshToken)
          || jwtService.isTokenExpired(refreshToken)) {
        throw new AccessDeniedException("Authentication required");
      }

      authenticatedUser = jwtService.extractUserDTO(refreshToken);
      verifyUserId(authenticatedUser, requestedUserId);

      // 3️⃣ Issue new access token
      String newAccessToken = jwtService.generateAccessToken(authenticatedUser);

      ResponseCookie cookie = ResponseCookie.from("access_token", newAccessToken)
          .httpOnly(true)
          .secure(false) // true in prod
          .sameSite("Lax")
          .path("/")
          .maxAge(Duration.ofMinutes(15))
          .build();

      response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
      request.setAttribute("authenticatedUser", authenticatedUser);

    } catch (AccessDeniedException e) {
      throw e;
    } catch (Exception e) {
      throw new AccessDeniedException("Authentication failed", e);
    }
  }
}
