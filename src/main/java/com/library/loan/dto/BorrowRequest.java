package com.library.loan.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BorrowRequest(
        @NotNull(message = "Book ID is required")
        UUID bookId
) {}
