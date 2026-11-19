package com.library.report.dto;

import java.time.LocalDate;
import java.util.UUID;

public record OverdueReportResponse(
        UUID loanId,
        UUID bookId,
        String bookTitle,
        String bookIsbn,
        UUID userId,
        String userEmail,
        String userFullName,
        LocalDate dueDate,
        long daysOverdue
) {}
