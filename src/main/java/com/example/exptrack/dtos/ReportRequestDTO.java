package com.example.exptrack.dtos;

public record ReportRequestDTO(
    String type,
    String startDate,
    String endDate,
    String format) {
}
