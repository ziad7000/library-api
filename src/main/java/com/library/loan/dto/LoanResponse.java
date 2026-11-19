package com.library.loan.dto;

import com.library.loan.model.Loan;
import com.library.loan.model.LoanStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LoanResponse(
        UUID id,
        UUID bookId,
        String bookTitle,
        String bookIsbn,
        UUID userId,
        String userEmail,
        String userFullName,
        Instant borrowedAt,
        LocalDate dueDate,
        Instant returnedAt,
        int renewalsCount,
        LoanStatus status,
        boolean overdue
) {
    public static LoanResponse from(Loan loan, LocalDate today) {
        boolean isOverdue = loan.getStatus() == LoanStatus.ACTIVE && loan.getDueDate().isBefore(today);
        return new LoanResponse(
                loan.getId(),
                loan.getBook().getId(),
                loan.getBook().getTitle(),
                loan.getBook().getIsbn(),
                loan.getUser().getId(),
                loan.getUser().getEmail(),
                loan.getUser().getFullName(),
                loan.getBorrowedAt(),
                loan.getDueDate(),
                loan.getReturnedAt(),
                loan.getRenewalsCount(),
                loan.getStatus(),
                isOverdue
        );
    }
}
