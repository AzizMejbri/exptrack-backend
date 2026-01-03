package com.example.exptrack.dtos;

import java.util.List;

public record ExpenseReportDTO(
    String category,
    Double totalAmount,
    Long transactionCount,
    Double averageAmount,
    Double percentage,
    List<MonthlyBreakdownDTO> monthlyBreakdown) {
}
