package com.example.exptrack.dtos;

// TrendAnalysisDTO.java
public record TrendAnalysisDTO(
    String period,
    Double totalAmount,
    Double percentageChange,
    String trend,
    Double forecast) {
}
