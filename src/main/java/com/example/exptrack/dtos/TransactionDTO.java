package com.example.exptrack.dtos;

import java.util.Date;

public class TransactionDTO {
  private Long id;
  private Double amount;
  private String type; // "expense" or "revenue"
  private String category; // For expenses
  private String source; // For revenues (renamed from category for revenues)
  private String description;
  private Date creationDate;
  private Date lastModified;

  // Additional fields for frontend compatibility
  private String transactionType; // Alias for type if needed

  public TransactionDTO(Long id, Double amount, String type, String category, String source, String description,
      Date creationDate, Date lastModified, String transactionType) {
    this.id = id;
    this.amount = amount;
    this.type = type;
    this.category = category;
    this.source = source;
    this.description = description;
    this.creationDate = creationDate;
    this.lastModified = lastModified;
    this.transactionType = transactionType;
  }

  public TransactionDTO() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  public String getTransactionType() {
    return transactionType;
  }

  public void setTransactionType(String transactionType) {
    this.transactionType = transactionType;
  }
}
