
package com.example.exptrack.controllers;

import com.example.exptrack.dtos.*;
import com.example.exptrack.services.TransactionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users/{userId}")
public class TransactionController {

  @Autowired
  private TransactionService transactionService;

  /* ===================== HELPERS ===================== */

  private void verifyUser(Authentication auth, Long requestedUserId) {
    UserDTO user = (UserDTO) auth.getPrincipal();
    System.out.println("user.id() = " + user.id());
    System.out.println("requestedUserId = " + requestedUserId);
    if (!user.id().equals(requestedUserId)) {
      throw new AccessDeniedException("Forbidden");
    }
  }

  /* ===================== TRANSACTIONS ===================== */

  @GetMapping("/transactions")
  public ResponseEntity<List<TransactionDTO>> getTransactions(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "month") String timeFrame,
      @RequestParam(defaultValue = "10") int limit,
      @RequestParam(defaultValue = "1") int page,
      Authentication auth) {

    verifyUser(auth, userId);
    return ResponseEntity.ok(
        transactionService.getTransactions(userId, timeFrame, limit, page));
  }

  @GetMapping("/transactions/recent")
  public ResponseEntity<List<TransactionDTO>> getRecentTransactions(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "5") int limit,
      Authentication auth) {

    verifyUser(auth, userId);
    return ResponseEntity.ok(
        transactionService.getRecentTransactions(userId, limit));
  }

  @GetMapping("/transactions/summary")
  public ResponseEntity<TransactionSummaryDTO> getTransactionSummary(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "month") String timeFrame,
      Authentication auth) {

    verifyUser(auth, userId);
    return ResponseEntity.ok(
        transactionService.getTransactionSummary(userId, timeFrame));
  }

  @GetMapping("/transactions/categories/summary")
  public ResponseEntity<List<CategorySummaryDTO>> getCategorySummary(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "month") String timeFrame,
      Authentication auth) {

    verifyUser(auth, userId);
    return ResponseEntity.ok(
        transactionService.getCategorySummary(userId, timeFrame));
  }

  @GetMapping("/transactions/categories/stats")
  public ResponseEntity<Map<String, Object>> getCategoryStats(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "month") String timeFrame,
      Authentication auth) {

    System.out.println("DEBUG: Entering getCategoryStats for user " + userId);

    verifyUser(auth, userId);
    System.err.println("ERROR: Failed After VerifyUser in getCategoryStats");
    return ResponseEntity.ok(
        transactionService.getCategoryStats(userId, timeFrame));
  }

  @PostMapping("/transactions")
  public ResponseEntity<TransactionDTO> addTransaction(
      @PathVariable Long userId,
      @RequestBody TransactionDTO transactionDTO,
      Authentication auth) {

    verifyUser(auth, userId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(transactionService.addTransaction(userId, transactionDTO));
  }

  @PutMapping("/transactions/{transactionId}")
  public ResponseEntity<TransactionDTO> updateTransaction(
      @PathVariable Long userId,
      @PathVariable Long transactionId,
      @RequestParam String type,
      @RequestBody TransactionDTO transactionDTO,
      Authentication auth) {

    verifyUser(auth, userId);
    return ResponseEntity.ok(
        transactionService.updateTransaction(transactionId, type, transactionDTO));
  }

  @DeleteMapping("/transactions/{transactionId}")
  public ResponseEntity<Void> deleteTransaction(
      @PathVariable Long userId,
      @PathVariable Long transactionId,
      @RequestParam String type,
      Authentication auth) {

    verifyUser(auth, userId);
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
      Authentication auth) {

    verifyUser(auth, userId);

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
      Authentication auth) {

    verifyUser(auth, userId);

    List<TransactionDTO> revenues = transactionService
        .getTransactions(userId, timeFrame, limit * 2, page)
        .stream()
        .filter(t -> "revenue".equals(t.getType()))
        .limit(limit)
        .collect(Collectors.toList());

    return ResponseEntity.ok(revenues);
  }
}
