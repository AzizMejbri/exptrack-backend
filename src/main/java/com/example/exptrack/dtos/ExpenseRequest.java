package com.example.exptrack.dtos;

// Create this class
public class ExpenseRequest {
  private Double amount;
  private String category;

  public ExpenseRequest(String category, Double amount) {
    this.category = category;
    this.amount = amount;
  }

  public ExpenseRequest() {
  }

  public Double getAmount() {
    return amount;
  }

  public void setAmount(Double amount) {
    this.amount = amount;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }
}
