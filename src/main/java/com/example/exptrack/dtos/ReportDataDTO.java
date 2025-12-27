package com.example.exptrack.dtos;

public class ReportDataDTO {
  public ReportDataDTO(String type, String startDate, String endDate, String format) {
    this.type = type;
    this.startDate = startDate;
    this.endDate = endDate;
    this.format = format;
  }

  public ReportDataDTO() {
  }

  private String type; // "expense", "revenue", or "all"
  private String startDate;
  private String endDate;
  private String format; // "pdf", "csv", "json"

  public String getType() {
    return type;
  }

  public String getStartDate() {
    return startDate;
  }

  public String getEndDate() {
    return endDate;
  }

  public String getFormat() {
    return format;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  public void setEndDate(String endDate) {
    this.endDate = endDate;
  }

  public void setFormat(String format) {
    this.format = format;
  }
}
