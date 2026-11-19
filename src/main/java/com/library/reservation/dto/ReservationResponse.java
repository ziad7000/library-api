package com.library.reservation.dto;

import com.library.reservation.model.Reservation;
import com.library.reservation.model.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
        UUID id,
        UUID bookId,
        String bookTitle,
        String bookIsbn,
        UUID userId,
        String userEmail,
        ReservationStatus status,
        Instant createdAt,
        Instant readyAt,
        Instant expiresAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getBook().getId(),
                reservation.getBook().getTitle(),
                reservation.getBook().getIsbn(),
                reservation.getUser().getId(),
                reservation.getUser().getEmail(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getReadyAt(),
                reservation.getExpiresAt()
        );
    }
}
