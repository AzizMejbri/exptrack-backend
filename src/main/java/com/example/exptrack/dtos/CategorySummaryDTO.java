package com.example.exptrack.dtos;

public class CategorySummaryDTO {
  private String name; // Category for expenses, Source for revenues
  private Double amount;
  private String type; // "expense" or "revenue"
  private Double percentage;

  public CategorySummaryDTO() {
  }

  // Constructor for JPQL queries
  public CategorySummaryDTO(String name, Double amount, String type, Double percentage) {
    this.name = name;
    this.amount = amount;
    this.type = type;
    this.percentage = percentage;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Double getAmount() {
    return amount;
  }

  public void setAmount(Double amount) {
    this.amount = amount;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Double getPercentage() {
    return percentage;
  }

  public void setPercentage(Double percentage) {
    this.percentage = percentage;
  }
}
