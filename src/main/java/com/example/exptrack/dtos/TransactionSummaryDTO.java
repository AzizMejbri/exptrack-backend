package com.example.exptrack.dtos;

public class TransactionSummaryDTO {
  private Double totalExpenses;
  private Double totalRevenue;
  private Long expenseCount;
  private Long revenueCount;
  private Double netAmount;
  private String period;
  private String currency = "USD";

  public TransactionSummaryDTO() {
  }

  public TransactionSummaryDTO(Double totalExpenses, Double totalRevenue, Long expenseCount, Long revenueCount,
      Double netAmount, String period, String currency) {
    this.totalExpenses = totalExpenses;
    this.totalRevenue = totalRevenue;
    this.expenseCount = expenseCount;
    this.revenueCount = revenueCount;
    this.netAmount = netAmount;
    this.period = period;
    this.currency = currency;
  }

  public Double getTotalExpenses() {
    return totalExpenses;
  }

  public void setTotalExpenses(Double totalExpenses) {
    this.totalExpenses = totalExpenses;
  }

  public Double getTotalRevenue() {
    return totalRevenue;
  }

  public void setTotalRevenue(Double totalRevenue) {
    this.totalRevenue = totalRevenue;
  }

  public Long getExpenseCount() {
    return expenseCount;
  }

  public void setExpenseCount(Long expenseCount) {
    this.expenseCount = expenseCount;
  }

  public Long getRevenueCount() {
    return revenueCount;
  }

  public void setRevenueCount(Long revenueCount) {
    this.revenueCount = revenueCount;
  }

  public Double getNetAmount() {
    return netAmount;
  }

  public void setNetAmount(Double netAmount) {
    this.netAmount = netAmount;
  }

  public String getPeriod() {
    return period;
  }

  public void setPeriod(String period) {
    this.period = period;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }
}
