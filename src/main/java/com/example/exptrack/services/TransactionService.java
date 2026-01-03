package com.example.exptrack.services;

import com.example.exptrack.dtos.*;
import com.example.exptrack.models.Expense;
import com.example.exptrack.models.Revenue;
import com.example.exptrack.models.User;
import com.example.exptrack.repositories.ExpenseRepository;
import com.example.exptrack.repositories.RevenueRepository;
import com.example.exptrack.repositories.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

  @Autowired
  private ReportGeneratorService reportGeneratorService;

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
    System.out.println("DEBUG: Starting TransactionService.getCategoryStats for user " + userId);
    try {
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
    } catch (Exception e) {
      System.err.println("ERROR in getCategoryStats: " + e.getMessage());
      e.printStackTrace();
      throw e; // Re-throw to see what happens
    }
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

  public List<TrendAnalysisDTO> getTrendAnalysis(Long userId, String timeFrame) {
    Map<String, Date> dateRange = getDateRange(timeFrame.equals("all") ? "year" : timeFrame);
    List<TransactionDTO> transactions = getTransactions(userId, timeFrame, 1000, 1);

    // Group by period based on timeFrame
    Map<String, List<TransactionDTO>> grouped = new TreeMap<>();

    DateTimeFormatter formatter;
    switch (timeFrame.toLowerCase()) {
      case "monthly":
        formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        break;
      case "quarterly":
        formatter = DateTimeFormatter.ofPattern("yyyy-QQQ");
        break;
      case "yearly":
        formatter = DateTimeFormatter.ofPattern("yyyy");
        break;
      default:
        formatter = DateTimeFormatter.ofPattern("yyyy-MM");
    }

    // Group transactions
    for (TransactionDTO t : transactions) {
      LocalDate date = t.getCreationDate().toInstant()
          .atZone(ZoneId.systemDefault())
          .toLocalDate();
      String period = date.format(formatter);
      grouped.computeIfAbsent(period, k -> new ArrayList<>()).add(t);
    }

    // Calculate trend analysis
    List<TrendAnalysisDTO> trendAnalysis = new ArrayList<>();
    Double previousAmount = null;

    for (Map.Entry<String, List<TransactionDTO>> entry : grouped.entrySet()) {
      Double totalAmount = entry.getValue().stream()
          .mapToDouble(TransactionDTO::getAmount)
          .sum();

      Double percentageChange = 0.0;
      if (previousAmount != null && previousAmount > 0) {
        percentageChange = ((totalAmount - previousAmount) / previousAmount) * 100;
      }

      String trend;
      if (percentageChange > 5)
        trend = "up";
      else if (percentageChange < -5)
        trend = "down";
      else
        trend = "stable";

      Double forecast = totalAmount * 1.1; // Simple 10% growth forecast

      trendAnalysis.add(new TrendAnalysisDTO(
          entry.getKey(),
          totalAmount,
          percentageChange,
          trend,
          forecast));

      previousAmount = totalAmount;
    }

    return trendAnalysis;
  }

  public List<ExpenseReportDTO> getExpenseReport(Long userId, String startDate, String endDate) {
    try {
      // Parse dates
      LocalDate start = LocalDate.parse(startDate);
      LocalDate end = LocalDate.parse(endDate);

      Date startDateObj = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
      Date endDateObj = Date.from(end.atStartOfDay(ZoneId.systemDefault()).toInstant());

      // ADD THIS LOGGING
      System.out.println("DEBUG: Querying expenses from " + startDateObj + " to " + endDateObj);

      List<Expense> expenses = expenseRepository.findByUserIdAndCreationDateBetween(
          userId,
          Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant()),
          Date.from(end.atStartOfDay(ZoneId.systemDefault()).toInstant()));

      System.out.println("DEBUG: Found " + expenses.size() + " expenses");
      // Group by category
      Map<String, List<Expense>> groupedByCategory = expenses.stream()
          .collect(Collectors.groupingBy(Expense::getCategory));

      Double totalExpenses = expenses.stream()
          .mapToDouble(Expense::getAmount)
          .sum();

      List<ExpenseReportDTO> report = new ArrayList<>();

      for (Map.Entry<String, List<Expense>> entry : groupedByCategory.entrySet()) {
        String category = entry.getKey() != null ? entry.getKey() : "Uncategorized";
        List<Expense> categoryExpenses = entry.getValue();

        Double categoryTotal = categoryExpenses.stream()
            .mapToDouble(Expense::getAmount)
            .sum();

        Double percentage = totalExpenses > 0 ? (categoryTotal / totalExpenses) * 100 : 0;

        // Monthly breakdown
        Map<String, List<Expense>> monthlyGrouped = categoryExpenses.stream()
            .collect(Collectors.groupingBy(e -> e.getCreationDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ofPattern("yyyy-MM"))));

        List<MonthlyBreakdownDTO> monthlyBreakdown = new ArrayList<>();
        for (Map.Entry<String, List<Expense>> monthEntry : monthlyGrouped.entrySet()) {
          Double monthTotal = monthEntry.getValue().stream()
              .mapToDouble(Expense::getAmount)
              .sum();
          Double monthPercentage = categoryTotal > 0 ? (monthTotal / categoryTotal) * 100 : 0;

          monthlyBreakdown.add(new MonthlyBreakdownDTO(
              monthEntry.getKey(),
              monthTotal,
              monthPercentage));
        }

        report.add(new ExpenseReportDTO(
            category,
            categoryTotal,
            (long) categoryExpenses.size(),
            categoryExpenses.size() > 0 ? categoryTotal / categoryExpenses.size() : 0,
            percentage,
            monthlyBreakdown));
      }

      return report;

    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public IncomeStatementDTO getIncomeStatement(Long userId, String startDate, String endDate) {
    LocalDate start = LocalDate.parse(startDate);
    LocalDate end = LocalDate.parse(endDate);

    Date startDateObj = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
    Date endDateObj = Date.from(end.atStartOfDay(ZoneId.systemDefault()).toInstant());

    // Get expenses
    List<Expense> expenses = expenseRepository.findByUserIdAndCreationDateBetween(
        userId, startDateObj, endDateObj);

    // Get revenues
    List<Revenue> revenues = revenueRepository.findByUserIdAndCreationDateBetween(
        userId, startDateObj, endDateObj);

    Double totalExpenses = expenses.stream()
        .mapToDouble(Expense::getAmount)
        .sum();

    Double totalRevenue = revenues.stream()
        .mapToDouble(Revenue::getAmount)
        .sum();

    Double netIncome = totalRevenue - totalExpenses;
    Double grossMargin = totalRevenue > 0 ? (netIncome / totalRevenue) * 100 : 0;

    // Group expenses by category
    Map<String, List<Expense>> expenseGroups = expenses.stream()
        .collect(Collectors.groupingBy(e -> e.getCategory() != null ? e.getCategory() : "Uncategorized"));

    List<CategoryBreakdownDTO> expenseCategories = expenseGroups.entrySet().stream()
        .map(entry -> {
          Double amount = entry.getValue().stream()
              .mapToDouble(Expense::getAmount)
              .sum();
          Double percentage = totalExpenses > 0 ? (amount / totalExpenses) * 100 : 0;

          return new CategoryBreakdownDTO(entry.getKey(), amount, percentage);
        })
        .collect(Collectors.toList());

    // Group revenues by source
    Map<String, List<Revenue>> revenueGroups = revenues.stream()
        .collect(Collectors.groupingBy(r -> r.getSource() != null ? r.getSource() : "Other"));

    List<CategoryBreakdownDTO> revenueCategories = revenueGroups.entrySet().stream()
        .map(entry -> {
          Double amount = entry.getValue().stream()
              .mapToDouble(Revenue::getAmount)
              .sum();
          Double percentage = totalRevenue > 0 ? (amount / totalRevenue) * 100 : 0;

          return new CategoryBreakdownDTO(entry.getKey(), amount, percentage);
        })
        .collect(Collectors.toList());

    Map<String, List<CategoryBreakdownDTO>> categories = new HashMap<>();
    categories.put("revenue", revenueCategories);
    categories.put("expenses", expenseCategories);

    return new IncomeStatementDTO(
        totalRevenue,
        totalExpenses,
        netIncome,
        grossMargin,
        categories);
  }

  // Update generateReport method in TransactionService
  public ResponseEntity<byte[]> generateReport(Long userId, ReportRequestDTO request) {
    try {
      // Generate report content based on type
      byte[] reportBytes;
      String contentType;
      String fileExtension;

      switch (request.format().toLowerCase()) {
        case "csv":
          // Prepare data for CSV
          Map<String, Object> csvData = prepareCsvData(userId, request);
          List<Map<String, Object>> csvRows = (List<Map<String, Object>>) csvData.get("rows");
          List<String> headers = (List<String>) csvData.get("headers");

          reportBytes = reportGeneratorService.generateCsv(
              request.type() + " Report",
              csvRows,
              headers);
          contentType = "text/csv";
          fileExtension = "csv";
          break;

        case "html":
          Map<String, Object> htmlData = prepareReportData(userId, request);
          reportBytes = reportGeneratorService.generateHtml(
              request.type() + " Report",
              htmlData);
          contentType = "text/html";
          fileExtension = "html";
          break;

        case "markdown":
          Map<String, Object> mdData = prepareReportData(userId, request);
          reportBytes = reportGeneratorService.generateMarkdown(
              request.type() + " Report",
              mdData);
          contentType = "text/markdown";
          fileExtension = "md";
          break;

        case "json":
          Object jsonData = prepareJsonData(userId, request);
          reportBytes = reportGeneratorService.generateJson(jsonData);
          contentType = "application/json";
          fileExtension = "json";
          break;

        case "pdf":
        default:
          // Simple text-based "PDF" for now
          Map<String, Object> pdfData = prepareReportData(userId, request);
          System.out.println("DEBUG PDF Data: " + pdfData);
          System.out.println("DEBUG Summary: " + pdfData.get("summary"));
          System.out.println("DEBUG Tables: " + pdfData.get("tables"));

          reportBytes = reportGeneratorService.generatePdf(
              request.type() + " Report",
              pdfData);
          contentType = "application/pdf";
          fileExtension = "pdf";
      }

      String filename = String.format("%s_%s_%s.%s",
          request.type(),
          userId,
          LocalDate.now().toString(),
          fileExtension);

      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_TYPE, contentType)
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
          .body(reportBytes);

    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.status(500).build();
    }
  }

  private Map<String, Object> prepareCsvData(Long userId, ReportRequestDTO request) {
    try {
      Map<String, Object> data = new HashMap<>();
      List<String> headers = new ArrayList<>();
      List<Map<String, Object>> rows = new ArrayList<>();

      if (request.type().equals("expense")) {
        List<ExpenseReportDTO> report = getExpenseReport(userId, request.startDate(), request.endDate());
        headers = List.of("Category", "Total Amount", "Transaction Count", "Average Amount", "Percentage");

        for (ExpenseReportDTO item : report) {
          Map<String, Object> row = new HashMap<>();
          row.put("Category", item.category());
          row.put("Total Amount", item.totalAmount());
          row.put("Transaction Count", item.transactionCount());
          row.put("Average Amount", item.averageAmount());
          row.put("Percentage", item.percentage());
          rows.add(row);
        }
      }
      // Add more types as needed...

      data.put("headers", headers);
      data.put("rows", rows);
      return data;

    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  private Map<String, Object> prepareReportData(Long userId, ReportRequestDTO request) {
    Map<String, Object> data = new HashMap<>();
    Map<String, Object> summary = new HashMap<>();
    List<Map<String, Object>> tables = new ArrayList<>();

    try {
      System.out.println("DEBUG: Preparing report data for type: " + request.type());

      if (request.type().equalsIgnoreCase("expense")) {
        List<ExpenseReportDTO> report = getExpenseReport(userId, request.startDate(), request.endDate());

        System.out.println("DEBUG: Got " + report.size() + " expense records");

        if (!report.isEmpty()) {
          double total = report.stream().mapToDouble(ExpenseReportDTO::totalAmount).sum();
          double avg = report.stream().mapToDouble(ExpenseReportDTO::averageAmount).average().orElse(0);

          summary.put("Total Expenses", total);
          summary.put("Average per Category", avg);
          summary.put("Number of Categories", report.size());

          // Create table
          Map<String, Object> table = new HashMap<>();
          table.put("title", "Expense Categories");
          table.put("headers", List.of("Category", "Amount", "Transactions", "Average", "Percentage"));

          List<List<Object>> rows = new ArrayList<>();
          for (ExpenseReportDTO item : report) {
            rows.add(List.of(
                item.category(),
                item.totalAmount(),
                item.transactionCount(),
                item.averageAmount(),
                item.percentage() + "%"));
          }
          table.put("rows", rows);
          tables.add(table);
        } else {
          summary.put("Message", "No expense data found for the selected period");
        }

      } else if (request.type().equalsIgnoreCase("income-statement")) {
        IncomeStatementDTO incomeStatement = getIncomeStatement(userId, request.startDate(), request.endDate());

        summary.put("Total Revenue", incomeStatement.totalRevenue());
        summary.put("Total Expenses", incomeStatement.totalExpenses());
        summary.put("Net Income", incomeStatement.netIncome());
        summary.put("Gross Margin", incomeStatement.grossMargin() + "%");

        // Revenue table
        if (incomeStatement.categories().containsKey("revenue")) {
          List<CategoryBreakdownDTO> revenues = incomeStatement.categories().get("revenue");
          if (!revenues.isEmpty()) {
            Map<String, Object> revenueTable = new HashMap<>();
            revenueTable.put("title", "Revenue Breakdown");
            revenueTable.put("headers", List.of("Source", "Amount", "Percentage"));

            List<List<Object>> revenueRows = new ArrayList<>();
            for (CategoryBreakdownDTO cat : revenues) {
              revenueRows.add(List.of(cat.name(), cat.amount(), cat.percentage() + "%"));
            }
            revenueTable.put("rows", revenueRows);
            tables.add(revenueTable);
          }
        }

        // Expense table
        if (incomeStatement.categories().containsKey("expenses")) {
          List<CategoryBreakdownDTO> expenses = incomeStatement.categories().get("expenses");
          if (!expenses.isEmpty()) {
            Map<String, Object> expenseTable = new HashMap<>();
            expenseTable.put("title", "Expense Breakdown");
            expenseTable.put("headers", List.of("Category", "Amount", "Percentage"));

            List<List<Object>> expenseRows = new ArrayList<>();
            for (CategoryBreakdownDTO cat : expenses) {
              expenseRows.add(List.of(cat.name(), cat.amount(), cat.percentage() + "%"));
            }
            expenseTable.put("rows", expenseRows);
            tables.add(expenseTable);
          }
        }

      } else if (request.type().equalsIgnoreCase("all") || request.type().equalsIgnoreCase("transactions")) {
        // Get both expenses and revenues
        List<Expense> expenses = expenseRepository.findByUserIdAndCreationDateBetween(
            userId,
            Date.from(LocalDate.parse(request.startDate()).atStartOfDay(ZoneId.systemDefault()).toInstant()),
            Date.from(LocalDate.parse(request.endDate()).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        List<Revenue> revenues = revenueRepository.findByUserIdAndCreationDateBetween(
            userId,
            Date.from(LocalDate.parse(request.startDate()).atStartOfDay(ZoneId.systemDefault()).toInstant()),
            Date.from(LocalDate.parse(request.endDate()).plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));

        System.out.println("DEBUG: Found " + expenses.size() + " expenses and " + revenues.size() + " revenues");

        double totalExpenses = expenses.stream().mapToDouble(Expense::getAmount).sum();
        double totalRevenues = revenues.stream().mapToDouble(Revenue::getAmount).sum();

        summary.put("Total Revenues", totalRevenues);
        summary.put("Total Expenses", totalExpenses);
        summary.put("Net Income", totalRevenues - totalExpenses);
        summary.put("Transaction Count", expenses.size() + revenues.size());

        // Create combined transactions table
        Map<String, Object> transTable = new HashMap<>();
        transTable.put("title", "All Transactions");
        transTable.put("headers", List.of("Date", "Type", "Category/Source", "Amount"));

        List<List<Object>> transRows = new ArrayList<>();

        // Add revenues
        for (Revenue r : revenues) {
          transRows.add(List.of(
              r.getCreationDate().toString(),
              "Revenue",
              r.getSource() != null ? r.getSource() : "N/A",
              r.getAmount()));
        }

        // Add expenses
        for (Expense e : expenses) {
          transRows.add(List.of(
              e.getCreationDate().toString(),
              "Expense",
              e.getCategory() != null ? e.getCategory() : "N/A",
              e.getAmount()));
        }

        // Sort by date (most recent first)
        transRows.sort((a, b) -> b.get(0).toString().compareTo(a.get(0).toString()));

        transTable.put("rows", transRows);
        tables.add(transTable);
      }

      data.put("summary", summary);
      data.put("tables", tables);

      System.out.println("DEBUG: Final data has " + summary.size() + " summary items and " + tables.size() + " tables");

      return data;

    } catch (Exception e) {
      System.err.println("ERROR in prepareReportData: " + e.getMessage());
      e.printStackTrace();

      // Return error data
      summary.put("Error", "Failed to generate report: " + e.getMessage());
      data.put("summary", summary);
      data.put("tables", tables);
      return data;
    }
  }

  private Object prepareJsonData(Long userId, ReportRequestDTO request) {
    Map<String, Object> jsonData = new HashMap<>();
    jsonData.put("reportType", request.type());
    jsonData.put("userId", userId);
    jsonData.put("period", request.startDate() + " to " + request.endDate());
    jsonData.put("generatedAt", new Date().toString());

    if (request.type().equals("expense")) {
      List<ExpenseReportDTO> report = getExpenseReport(userId, request.startDate(), request.endDate());
      jsonData.put("data", report);
    }

    return jsonData;
  }

  private String generateReportContent(Long userId, ReportRequestDTO request) {
    // Generate report content based on type
    StringBuilder content = new StringBuilder();
    content.append("Financial Report\n");
    content.append("================\n\n");
    content.append("User ID: ").append(userId).append("\n");
    content.append("Period: ").append(request.startDate()).append(" to ").append(request.endDate()).append("\n");
    content.append("Type: ").append(request.type()).append("\n");
    content.append("Generated: ").append(new Date()).append("\n\n");

    // Add report data based on type
    if (request.type().equals("expense")) {
      List<ExpenseReportDTO> expenseReport = getExpenseReport(userId, request.startDate(), request.endDate());
      content.append("Expense Report:\n");
      for (ExpenseReportDTO item : expenseReport) {
        content.append(String.format("- %s: $%.2f (%d transactions)\n",
            item.category(), item.totalAmount(), item.transactionCount()));
      }
    } else if (request.type().equals("income-statement")) {
      IncomeStatementDTO incomeStatement = getIncomeStatement(userId, request.startDate(), request.endDate());
      content.append("Income Statement:\n");
      content.append(String.format("Total Revenue: $%.2f\n", incomeStatement.totalRevenue()));
      content.append(String.format("Total Expenses: $%.2f\n", incomeStatement.totalExpenses()));
      content.append(String.format("Net Income: $%.2f\n", incomeStatement.netIncome()));
      content.append(String.format("Gross Margin: %.1f%%\n", incomeStatement.grossMargin()));
    }

    return content.toString();
  }

  public Map<String, Object> getBudgetVsActual(Long userId, String timeFrame) {
    // This would compare budget vs actual expenses
    // For now, return a simple structure
    Map<String, Object> result = new HashMap<>();

    // Get actual expenses
    Map<String, Date> dateRange = getDateRange(timeFrame);
    List<Expense> expenses = expenseRepository.findByUserIdAndCreationDateBetween(
        userId, dateRange.get("start"), dateRange.get("end"));

    // Group by category
    Map<String, Double> actualByCategory = expenses.stream()
        .collect(Collectors.groupingBy(
            e -> e.getCategory() != null ? e.getCategory() : "Uncategorized",
            Collectors.summingDouble(Expense::getAmount)));

    // Get budget (you would have a budget table - for now use default budgets)
    Map<String, Double> budgetByCategory = new HashMap<>();
    actualByCategory.keySet().forEach(category -> budgetByCategory.put(category, 500.0) // Default budget of $500 per
                                                                                        // category
    );

    // Calculate variances
    List<Map<String, Object>> categories = new ArrayList<>();
    for (Map.Entry<String, Double> entry : actualByCategory.entrySet()) {
      String category = entry.getKey();
      Double actual = entry.getValue();
      Double budget = budgetByCategory.getOrDefault(category, 0.0);
      Double variance = actual - budget;
      Double variancePercent = budget > 0 ? (variance / budget) * 100 : 0;

      Map<String, Object> categoryData = new HashMap<>();
      categoryData.put("category", category);
      categoryData.put("budget", budget);
      categoryData.put("actual", actual);
      categoryData.put("variance", variance);
      categoryData.put("variancePercent", variancePercent);
      categories.add(categoryData);
    }

    Double totalBudget = budgetByCategory.values().stream().mapToDouble(Double::doubleValue).sum();
    Double totalActual = actualByCategory.values().stream().mapToDouble(Double::doubleValue).sum();
    Double totalVariance = totalActual - totalBudget;
    Double totalVariancePercent = totalBudget > 0 ? (totalVariance / totalBudget) * 100 : 0;

    Map<String, Object> total = new HashMap<>();
    total.put("budget", totalBudget);
    total.put("actual", totalActual);
    total.put("variance", totalVariance);
    total.put("variancePercent", totalVariancePercent);

    result.put("categories", categories);
    result.put("total", total);
    result.put("timeFrame", timeFrame);

    return result;
  }

}
