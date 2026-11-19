package com.library.fine.dto;

import com.library.fine.model.Fine;
import com.library.fine.model.FineStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FineResponse(
        UUID id,
        UUID loanId,
        UUID bookId,
        String bookTitle,
        UUID userId,
        String userEmail,
        BigDecimal amount,
        FineStatus status,
        Instant createdAt,
        Instant paidAt
) {
    public static FineResponse from(Fine fine) {
        return new FineResponse(
                fine.getId(),
                fine.getLoan().getId(),
                fine.getLoan().getBook().getId(),
                fine.getLoan().getBook().getTitle(),
                fine.getUser().getId(),
                fine.getUser().getEmail(),
                fine.getAmount(),
                fine.getStatus(),
                fine.getCreatedAt(),
                fine.getPaidAt()
        );
    }
}
