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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/api/users/{userId}")
@SecurityRequirement(name = "cookieAuth") // All endpoints here require authentication
public class TransactionController {

  @Autowired
  private TransactionService transactionService;

  /* ===================== HELPERS ===================== */

  private void verifyUser(Authentication auth, Long requestedUserId) {
    UserDTO user = (UserDTO) auth.getPrincipal();
    if (!user.id().equals(requestedUserId)) {
      throw new AccessDeniedException("Forbidden");
    }
  }

  /* ===================== TRANSACTIONS ===================== */

  @GetMapping("/transactions")
  @Operation(summary = "Get all transactions for a user", description = "Fetches transactions for a given user, with optional timeframe, limit, and pagination.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "List of transactions returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionDTO.class))),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<List<TransactionDTO>> getTransactions(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "month") String timeFrame,
      @RequestParam(defaultValue = "10") int limit,
      @RequestParam(defaultValue = "1") int page,
      Authentication auth) {

    verifyUser(auth, userId);
    return ResponseEntity.ok(transactionService.getTransactions(userId, timeFrame, limit, page));
  }

  @GetMapping("/transactions/recent")
  @Operation(summary = "Get recent transactions", description = "Fetch the most recent transactions for a user, limited by count.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "List of recent transactions returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionDTO.class))),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<List<TransactionDTO>> getRecentTransactions(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "5") int limit,
      Authentication auth) {

    verifyUser(auth, userId);
    return ResponseEntity.ok(transactionService.getRecentTransactions(userId, limit));
  }

  @GetMapping("/transactions/summary")
  @Operation(summary = "Get transaction summary", description = "Returns a summary of transactions for a user over a given timeframe.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Transaction summary returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionSummaryDTO.class))),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<TransactionSummaryDTO> getTransactionSummary(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "month") String timeFrame,
      Authentication auth) {

    verifyUser(auth, userId);
    return ResponseEntity.ok(transactionService.getTransactionSummary(userId, timeFrame));
  }

  @GetMapping("/transactions/categories/summary")
  @Operation(summary = "Get category summary", description = "Returns summary statistics grouped by transaction categories.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Category summary returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategorySummaryDTO.class))),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<List<CategorySummaryDTO>> getCategorySummary(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "month") String timeFrame,
      Authentication auth) {

    verifyUser(auth, userId);
    return ResponseEntity.ok(transactionService.getCategorySummary(userId, timeFrame));
  }

  @GetMapping("/transactions/categories/stats")
  @Operation(summary = "Get category statistics", description = "Returns detailed stats for categories, including counts, sums, and averages.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Category stats returned", content = @Content(mediaType = "application/json")),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<Map<String, Object>> getCategoryStats(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "month") String timeFrame,
      Authentication auth) {

    verifyUser(auth, userId);
    return ResponseEntity.ok(transactionService.getCategoryStats(userId, timeFrame));
  }

  @PostMapping("/transactions")
  @Operation(summary = "Add a new transaction", description = "Creates a new transaction for the specified user.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Transaction created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionDTO.class))),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<TransactionDTO> addTransaction(
      @PathVariable Long userId,
      @RequestBody TransactionDTO transactionDTO,
      Authentication auth) {

    verifyUser(auth, userId);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(transactionService.addTransaction(userId, transactionDTO));
  }

  @PutMapping("/transactions/{transactionId}")
  @Operation(summary = "Update a transaction", description = "Updates an existing transaction by its ID and type.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Transaction updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionDTO.class))),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<TransactionDTO> updateTransaction(
      @PathVariable Long userId,
      @PathVariable Long transactionId,
      @RequestParam String type,
      @RequestBody TransactionDTO transactionDTO,
      Authentication auth) {

    verifyUser(auth, userId);
    return ResponseEntity.ok(transactionService.updateTransaction(transactionId, type, transactionDTO));
  }

  @DeleteMapping("/transactions/{transactionId}")
  @Operation(summary = "Delete a transaction", description = "Deletes a transaction by its ID and type.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Transaction deleted"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
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
  @Operation(summary = "Get user expenses", description = "Fetch only transactions of type 'expense'.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "List of expenses returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionDTO.class))),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<List<TransactionDTO>> getExpenses(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "month") String timeFrame,
      @RequestParam(defaultValue = "10") int limit,
      @RequestParam(defaultValue = "1") int page,
      Authentication auth) {

    verifyUser(auth, userId);
    List<TransactionDTO> expenses = transactionService.getTransactions(userId, timeFrame, limit * 2, page)
        .stream()
        .filter(t -> "expense".equals(t.getType()))
        .limit(limit)
        .collect(Collectors.toList());
    return ResponseEntity.ok(expenses);
  }

  @GetMapping("/revenues")
  @Operation(summary = "Get user revenues", description = "Fetch only transactions of type 'revenue'.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "List of revenues returned", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionDTO.class))),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<List<TransactionDTO>> getRevenues(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "month") String timeFrame,
      @RequestParam(defaultValue = "10") int limit,
      @RequestParam(defaultValue = "1") int page,
      Authentication auth) {

    verifyUser(auth, userId);
    List<TransactionDTO> revenues = transactionService.getTransactions(userId, timeFrame, limit * 2, page)
        .stream()
        .filter(t -> "revenue".equals(t.getType()))
        .limit(limit)
        .collect(Collectors.toList());
    return ResponseEntity.ok(revenues);
  }

  @GetMapping("/transactions/analysis/trend")
  @Operation(summary = "Trend analysis", description = "Returns trend analysis of user's transactions over a given timeframe.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Trend analysis returned"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<List<TrendAnalysisDTO>> getTrendAnalysis(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "monthly") String timeFrame,
      Authentication auth) {

    verifyUser(auth, userId);
    return ResponseEntity.ok(transactionService.getTrendAnalysis(userId, timeFrame));
  }

  @GetMapping("/transactions/reports/expense")
  @Operation(summary = "Expense report", description = "Returns an expense report between given start and end dates.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Expense report returned"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<List<ExpenseReportDTO>> getExpenseReport(
      @PathVariable Long userId,
      @RequestParam String startDate,
      @RequestParam String endDate,
      Authentication auth) {

    verifyUser(auth, userId);
    return ResponseEntity.ok(transactionService.getExpenseReport(userId, startDate, endDate));
  }

  @GetMapping("/transactions/reports/income-statement")
  @Operation(summary = "Income statement report", description = "Returns the user's income statement between start and end dates.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Income statement returned"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<IncomeStatementDTO> getIncomeStatement(
      @PathVariable Long userId,
      @RequestParam String startDate,
      @RequestParam String endDate,
      Authentication auth) {

    verifyUser(auth, userId);
    return ResponseEntity.ok(transactionService.getIncomeStatement(userId, startDate, endDate));
  }

  @PostMapping("/transactions/reports/generate")
  @Operation(summary = "Generate report", description = "Generates a PDF or Excel report based on the request body parameters.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Report generated"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<byte[]> generateReport(
      @PathVariable Long userId,
      @RequestBody ReportRequestDTO reportRequest,
      Authentication auth) {

    verifyUser(auth, userId);
    return transactionService.generateReport(userId, reportRequest);
  }

  @GetMapping("/transactions/analysis/budget-vs-actual")
  @Operation(summary = "Budget vs actual analysis", description = "Returns a comparison of budgeted amounts versus actual spending for a user.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Budget vs actual returned"),
      @ApiResponse(responseCode = "403", description = "Access denied")
  })
  public ResponseEntity<Map<String, Object>> getBudgetVsActual(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "month") String timeFrame,
      Authentication auth) {

    verifyUser(auth, userId);
    return ResponseEntity.ok(transactionService.getBudgetVsActual(userId, timeFrame));
  }
}
