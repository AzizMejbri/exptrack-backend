package com.example.exptrack.models;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Transaction {

  @Column(name = "amount", nullable = false)
  protected Double amount;

  @Column(name = "creation_date", nullable = false)
  protected Date creationDate;

  @Column(name = "last_modified", nullable = false)
  protected Date lastModified;

  @JoinColumn(name = "user_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  @JsonIgnore
  protected User user;

  public Transaction() {
  }

  public Transaction(Double amount, Date creationDate, Date lastModified, User user) {
    this.amount = amount;
    this.creationDate = creationDate;
    this.lastModified = lastModified;
    this.user = user;
  }

  public Double getAmount() {
    return amount;
  }

  public void setAmount(Double amount) {
    this.amount = amount;
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

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }
}
