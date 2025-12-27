package com.example.exptrack.models;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "expenses")
public class Expense extends Transaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long Id;

  @Column(name = "category")
  private String category;

  public Expense() {
  }

  public Expense(Long id, String category) {
    Id = id;
    this.category = category;
  }

  public Expense(Double amount, User user, Date creationDate, Date lastModified, String category) {
    this.amount = amount;
    this.user = user;
    this.creationDate = creationDate;
    this.lastModified = lastModified;
    this.category = category;
  }

  public Long getId() {
    return Id;
  }

  public void setId(Long id) {
    Id = id;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

}
