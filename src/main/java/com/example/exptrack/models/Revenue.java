package com.example.exptrack.models;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "revenue")
public class Revenue extends Transaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "source")
  private String source;

  public Revenue(
      Double amount,
      Date creationDate,
      Date lastModified,
      User user,
      Long id,
      String source) {
    super(
        amount,
        creationDate,
        lastModified,
        user);
    this.id = id;
    this.source = source;
  }

  public Revenue() {
    super();
  }

  public Revenue(Double amount, User user, Date creationDate, Date lastModified, String source) {
    this.amount = amount;
    this.user = user;
    this.creationDate = creationDate;
    this.lastModified = lastModified;
    this.source = source;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }
}
