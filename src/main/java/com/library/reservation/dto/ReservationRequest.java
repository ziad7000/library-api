package com.library.reservation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReservationRequest(
        @NotNull(message = "Book ID is required")
        UUID bookId
) {}
