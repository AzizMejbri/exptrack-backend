package com.example.exptrack.services;

import com.example.exptrack.dtos.*;
import com.example.exptrack.models.Expense;
import com.example.exptrack.models.Revenue;
import com.example.exptrack.models.User;
import com.example.exptrack.repositories.ExpenseRepository;
import com.example.exptrack.repositories.RevenueRepository;
import com.example.exptrack.repositories.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Date;

@Service
@Transactional
public class TransactionService {

  @Autowired
  private ExpenseRepository expenseRepository;
  @Autowired
  private RevenueRepository revenueRepository;
  @Autowired
  private UserRepository userRepository;

  // Helper method to get date range based on timeFrame
  private Map<String, Date> getDateRange(String timeFrame) {
    LocalDate now = LocalDate.now();
    LocalDate startDate;
    LocalDate endDate = now;

    switch (timeFrame.toLowerCase()) {
      case "day":
        startDate = now;
        break;
      case "week":
        startDate = now.minusDays(7);
        break;
      case "month":
        startDate = now.withDayOfMonth(1);
        break;
      case "year":
        startDate = now.withDayOfYear(1);
        break;
      case "all":
        startDate = LocalDate.of(2000, 1, 1); // Arbitrary early date
        break;
      default:
        startDate = now.withDayOfMonth(1); // Default to month
    }

    Map<String, Date> range = new HashMap<>();
    range.put("start", Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    range.put("end", Date.from(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
    return range;
  }

  // Get transactions with filtering (combined expenses and revenues)
  public List<TransactionDTO> getTransactions(Long userId, String timeFrame, int limit, int page) {
    Map<String, Date> dateRange = getDateRange(timeFrame);

    // Get expenses and revenues
    List<Expense> expenses = expenseRepository.findByUserIdAndCreationDateBetweenOrderByCreationDateDesc(
        userId, dateRange.get("start"), dateRange.get("end"));
    List<Revenue> revenues = revenueRepository.findByUserIdAndCreationDateBetweenOrderByCreationDateDesc(
        userId, dateRange.get("start"), dateRange.get("end"));

    // Combine and sort by creation date
    List<TransactionDTO> allTransactions = new ArrayList<>();

    // Convert expenses to DTOs
    expenses.forEach(expense -> {
      TransactionDTO dto = new TransactionDTO();
      dto.setId(expense.getId());
      dto.setAmount(expense.getAmount());
      dto.setType("expense");
      dto.setCategory(expense.getCategory());
      dto.setDescription("Expense: " + expense.getCategory()); // You might want to add description field
      dto.setCreationDate(expense.getCreationDate());
      dto.setLastModified(expense.getLastModified());
      dto.setTransactionType("expense");
      allTransactions.add(dto);
    });

    // Convert revenues to DTOs
    revenues.forEach(revenue -> {
      TransactionDTO dto = new TransactionDTO();
      dto.setId(revenue.getId());
      dto.setAmount(revenue.getAmount());
      dto.setType("revenue");
      dto.setSource(revenue.getSource());
      dto.setDescription("Revenue: " + revenue.getSource()); // You might want to add description field
      dto.setCreationDate(revenue.getCreationDate());
      dto.setLastModified(revenue.getLastModified());
      dto.setTransactionType("revenue");
      allTransactions.add(dto);
    });

    // Sort by creation date (most recent first)
    allTransactions.sort((a, b) -> b.getCreationDate().compareTo(a.getCreationDate()));

    // Apply pagination
    int start = (page - 1) * limit;
    int end = Math.min(start + limit, allTransactions.size());

    if (start >= allTransactions.size()) {
      return Collections.emptyList();
    }

    return allTransactions.subList(start, end);
  }

  // Get transaction summary
  public TransactionSummaryDTO getTransactionSummary(Long userId, String timeFrame) {
    Map<String, Date> dateRange = getDateRange(timeFrame);

    Double totalExpenses = expenseRepository.sumExpensesByUserAndDateRange(
        userId, dateRange.get("start"), dateRange.get("end"));
    Double totalRevenue = revenueRepository.sumRevenueByUserAndDateRange(
        userId, dateRange.get("start"), dateRange.get("end"));

    Long expenseCount = expenseRepository.countExpensesByUserAndDateRange(
        userId, dateRange.get("start"), dateRange.get("end"));
    Long revenueCount = revenueRepository.countRevenueByUserAndDateRange(
        userId, dateRange.get("start"), dateRange.get("end"));

    Double netAmount = totalRevenue - totalExpenses;

    TransactionSummaryDTO summary = new TransactionSummaryDTO();
    summary.setTotalExpenses(totalExpenses != null ? totalExpenses : 0.0);
    summary.setTotalRevenue(totalRevenue != null ? totalRevenue : 0.0);
    summary.setExpenseCount(expenseCount != null ? expenseCount : 0L);
    summary.setRevenueCount(revenueCount != null ? revenueCount : 0L);
    summary.setNetAmount(netAmount != null ? netAmount : 0.0);
    summary.setPeriod(timeFrame);
    summary.setCurrency("USD");

    return summary;
  }

  // Get category breakdown (combines expense categories and revenue sources)
  public List<CategorySummaryDTO> getCategorySummary(Long userId, String timeFrame) {
    Map<String, Date> dateRange = getDateRange(timeFrame);

    // Get expense categories
    List<CategorySummaryDTO> expenseCategories = expenseRepository.getExpenseCategorySummary(
        userId, dateRange.get("start"), dateRange.get("end"));

    // Get revenue sources
    List<CategorySummaryDTO> revenueSources = revenueRepository.getRevenueCategorySummary(
        userId, dateRange.get("start"), dateRange.get("end"));

    // Combine lists
    List<CategorySummaryDTO> allCategories = new ArrayList<>();
    allCategories.addAll(expenseCategories);
    allCategories.addAll(revenueSources);

    // Calculate percentages for each type
    Map<String, Double> typeTotals = new HashMap<>();
    typeTotals.put("expense", 0.0);
    typeTotals.put("revenue", 0.0);

    // Calculate totals for each type
    for (CategorySummaryDTO category : allCategories) {
      String type = category.getType();
      Double currentTotal = typeTotals.get(type);
      typeTotals.put(type, currentTotal + (category.getAmount() != null ? category.getAmount() : 0.0));
    }

    // Calculate percentages
    for (CategorySummaryDTO category : allCategories) {
      Double typeTotal = typeTotals.get(category.getType());
      if (typeTotal > 0 && category.getAmount() != null) {
        double percentage = (category.getAmount() / typeTotal) * 100;
        category.setPercentage(Math.round(percentage * 10.0) / 10.0); // Round to 1 decimal
      } else {
        category.setPercentage(0.0);
      }
    }

    return allCategories;
  }

  // Get detailed category statistics
  public Map<String, Object> getCategoryStats(Long userId, String timeFrame) {
    Map<String, Date> dateRange = getDateRange(timeFrame);
    List<CategorySummaryDTO> allCategories = getCategorySummary(userId, timeFrame);

    // Separate expenses and revenues
    List<CategorySummaryDTO> expenseCategories = allCategories.stream()
        .filter(c -> "expense".equals(c.getType()))
        .collect(Collectors.toList());

    List<CategorySummaryDTO> revenueCategories = allCategories.stream()
        .filter(c -> "revenue".equals(c.getType()))
        .collect(Collectors.toList());

    // Get expense category details with transaction counts
    List<Map<String, Object>> expenseCategoryDetails = new ArrayList<>();
    for (CategorySummaryDTO category : expenseCategories) {
      Map<String, Object> detail = new HashMap<>();
      detail.put("category", category.getName());
      detail.put("type", "expense");
      detail.put("totalAmount", category.getAmount());

      // Get transaction count for this category
      List<Expense> expenses = expenseRepository.findByUserIdAndCreationDateBetweenOrderByCreationDateDesc(
          userId, dateRange.get("start"), dateRange.get("end"));
      long categoryCount = expenses.stream()
          .filter(e -> category.getName().equals(e.getCategory()))
          .count();

      detail.put("transactionCount", categoryCount);
      detail.put("averageAmount", categoryCount > 0 ? category.getAmount() / categoryCount : 0);
      detail.put("percentage", category.getPercentage());
      expenseCategoryDetails.add(detail);
    }

    // Get revenue category details with transaction counts
    List<Map<String, Object>> revenueCategoryDetails = new ArrayList<>();
    for (CategorySummaryDTO category : revenueCategories) {
      Map<String, Object> detail = new HashMap<>();
      detail.put("category", category.getName());
      detail.put("type", "revenue");
      detail.put("totalAmount", category.getAmount());

      // Get transaction count for this source
      List<Revenue> revenues = revenueRepository.findByUserIdAndCreationDateBetweenOrderByCreationDateDesc(
          userId, dateRange.get("start"), dateRange.get("end"));
      long categoryCount = revenues.stream()
          .filter(r -> category.getName().equals(r.getSource()))
          .count();

      detail.put("transactionCount", categoryCount);
      detail.put("averageAmount", categoryCount > 0 ? category.getAmount() / categoryCount : 0);
      detail.put("percentage", category.getPercentage());
      revenueCategoryDetails.add(detail);
    }

    Map<String, Object> stats = new HashMap<>();
    stats.put("expenseCategories", expenseCategoryDetails);
    stats.put("revenueCategories", revenueCategoryDetails);
    stats.put("timeFrame", timeFrame);

    // Calculate summary
    Double totalExpenses = expenseCategories.stream()
        .mapToDouble(c -> c.getAmount() != null ? c.getAmount() : 0.0)
        .sum();

    Double totalRevenue = revenueCategories.stream()
        .mapToDouble(c -> c.getAmount() != null ? c.getAmount() : 0.0)
        .sum();

    long totalTransactions = expenseCategories.size() + revenueCategories.size();
    Double averageTransaction = totalTransactions > 0
        ? (totalExpenses + totalRevenue) / totalTransactions
        : 0.0;

    String mostSpentCategory = expenseCategories.stream()
        .max(Comparator.comparingDouble(c -> c.getAmount() != null ? c.getAmount() : 0.0))
        .map(CategorySummaryDTO::getName)
        .orElse("None");

    String mostRevenueCategory = revenueCategories.stream()
        .max(Comparator.comparingDouble(c -> c.getAmount() != null ? c.getAmount() : 0.0))
        .map(CategorySummaryDTO::getName)
        .orElse("None");

    Map<String, Object> summary = new HashMap<>();
    summary.put("totalExpenses", totalExpenses);
    summary.put("totalRevenue", totalRevenue);
    summary.put("averageTransaction", Math.round(averageTransaction * 100.0) / 100.0);
    summary.put("mostSpentCategory", mostSpentCategory);
    summary.put("mostRevenueCategory", mostRevenueCategory);

    stats.put("summary", summary);

    return stats;
  }

  // Add expense
  public TransactionDTO addExpense(Long userId, TransactionDTO transactionDTO) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));

    Date now = new Date();
    Expense expense = new Expense();
    expense.setAmount(transactionDTO.getAmount());
    expense.setUser(user);
    expense.setCreationDate(now);
    expense.setLastModified(now);
    expense.setCategory(transactionDTO.getCategory());

    Expense saved = expenseRepository.save(expense);

    return convertExpenseToDTO(saved);
  }

  // Add revenue
  public TransactionDTO addRevenue(Long userId, TransactionDTO transactionDTO) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));

    Date now = new Date();
    Revenue revenue = new Revenue();
    revenue.setAmount(transactionDTO.getAmount());
    revenue.setUser(user);
    revenue.setCreationDate(now);
    revenue.setLastModified(now);
    revenue.setSource(transactionDTO.getSource());

    Revenue saved = revenueRepository.save(revenue);

    return convertRevenueToDTO(saved);
  }

  // Add transaction (automatically determines type based on DTO)
  public TransactionDTO addTransaction(Long userId, TransactionDTO transactionDTO) {
    if ("expense".equalsIgnoreCase(transactionDTO.getType())) {
      return addExpense(userId, transactionDTO);
    } else if ("revenue".equalsIgnoreCase(transactionDTO.getType())) {
      return addRevenue(userId, transactionDTO);
    } else {
      throw new IllegalArgumentException("Transaction type must be 'expense' or 'revenue'");
    }
  }

  // Update expense
  public TransactionDTO updateExpense(Long expenseId, TransactionDTO transactionDTO) {
    Expense expense = expenseRepository.findById(expenseId)
        .orElseThrow(() -> new RuntimeException("Expense not found"));

    if (transactionDTO.getAmount() != null) {
      expense.setAmount(transactionDTO.getAmount());
    }
    if (transactionDTO.getCategory() != null) {
      expense.setCategory(transactionDTO.getCategory());
    }
    expense.setLastModified(new Date());

    Expense updated = expenseRepository.save(expense);
    return convertExpenseToDTO(updated);
  }

  // Update revenue
  public TransactionDTO updateRevenue(Long revenueId, TransactionDTO transactionDTO) {
    Revenue revenue = revenueRepository.findById(revenueId)
        .orElseThrow(() -> new RuntimeException("Revenue not found"));

    if (transactionDTO.getAmount() != null) {
      revenue.setAmount(transactionDTO.getAmount());
    }
    if (transactionDTO.getSource() != null) {
      revenue.setSource(transactionDTO.getSource());
    }
    revenue.setLastModified(new Date());

    Revenue updated = revenueRepository.save(revenue);
    return convertRevenueToDTO(updated);
  }

  // Update transaction (automatically determines type)
  public TransactionDTO updateTransaction(Long transactionId, String type, TransactionDTO transactionDTO) {
    if ("expense".equalsIgnoreCase(type)) {
      return updateExpense(transactionId, transactionDTO);
    } else if ("revenue".equalsIgnoreCase(type)) {
      return updateRevenue(transactionId, transactionDTO);
    } else {
      throw new IllegalArgumentException("Transaction type must be 'expense' or 'revenue'");
    }
  }

  // Delete expense
  public void deleteExpense(Long expenseId) {
    expenseRepository.deleteById(expenseId);
  }

  // Delete revenue
  public void deleteRevenue(Long revenueId) {
    revenueRepository.deleteById(revenueId);
  }

  // Delete transaction
  public void deleteTransaction(Long transactionId, String type) {
    if ("expense".equalsIgnoreCase(type)) {
      deleteExpense(transactionId);
    } else if ("revenue".equalsIgnoreCase(type)) {
      deleteRevenue(transactionId);
    } else {
      throw new IllegalArgumentException("Transaction type must be 'expense' or 'revenue'");
    }
  }

  // Helper methods to convert entities to DTOs
  private TransactionDTO convertExpenseToDTO(Expense expense) {
    TransactionDTO dto = new TransactionDTO();
    dto.setId(expense.getId());
    dto.setAmount(expense.getAmount());
    dto.setType("expense");
    dto.setCategory(expense.getCategory());
    dto.setDescription("Expense: " + expense.getCategory());
    dto.setCreationDate(expense.getCreationDate());
    dto.setLastModified(expense.getLastModified());
    dto.setTransactionType("expense");
    return dto;
  }

  private TransactionDTO convertRevenueToDTO(Revenue revenue) {
    TransactionDTO dto = new TransactionDTO();
    dto.setId(revenue.getId());
    dto.setAmount(revenue.getAmount());
    dto.setType("revenue");
    dto.setSource(revenue.getSource());
    dto.setDescription("Revenue: " + revenue.getSource());
    dto.setCreationDate(revenue.getCreationDate());
    dto.setLastModified(revenue.getLastModified());
    dto.setTransactionType("revenue");
    return dto;
  }

  // Get recent transactions (for dashboard)
  public List<TransactionDTO> getRecentTransactions(Long userId, int limit) {
    List<Expense> recentExpenses = expenseRepository.findByUserIdOrderByCreationDateDesc(userId);
    List<Revenue> recentRevenues = revenueRepository.findByUserIdOrderByCreationDateDesc(userId);

    List<TransactionDTO> allTransactions = new ArrayList<>();

    // Convert recent expenses
    recentExpenses.stream().limit(limit).forEach(expense -> {
      allTransactions.add(convertExpenseToDTO(expense));
    });

    // Convert recent revenues
    recentRevenues.stream().limit(limit).forEach(revenue -> {
      allTransactions.add(convertRevenueToDTO(revenue));
    });

    // Sort by creation date and limit
    return allTransactions.stream()
        .sorted((a, b) -> b.getCreationDate().compareTo(a.getCreationDate()))
        .limit(limit)
        .collect(Collectors.toList());
  }
}
