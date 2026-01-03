package com.example.exptrack.dtos;

import java.util.List;
import java.util.Map;

public record IncomeStatementDTO(
    Double totalRevenue,
    Double totalExpenses,
    Double netIncome,
    Double grossMargin,
    Map<String, List<CategoryBreakdownDTO>> categories) {
}
