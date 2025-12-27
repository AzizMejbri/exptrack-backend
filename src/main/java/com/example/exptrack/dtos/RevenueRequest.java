package com.example.exptrack.dtos;

public class RevenueRequest {
  private Double amount;
  private String source;

  public RevenueRequest(Double amount, String source) {
    this.amount = amount;
    this.source = source;
  }

  public RevenueRequest() {
  }

  public Double getAmount() {
    return amount;
  }

  public void setAmount(Double amount) {
    this.amount = amount;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }
}
