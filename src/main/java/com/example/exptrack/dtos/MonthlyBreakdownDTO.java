package com.example.exptrack.dtos;

public record MonthlyBreakdownDTO(
    String month,
    Double amount,
    Double percentage) {
}
