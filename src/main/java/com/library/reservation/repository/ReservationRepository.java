package com.library.reservation.repository;

import com.library.reservation.model.Reservation;
import com.library.reservation.model.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    boolean existsByBookIdAndUserIdAndStatusIn(UUID bookId, UUID userId, Collection<ReservationStatus> statuses);

    boolean existsByBookIdAndStatusIn(UUID bookId, Collection<ReservationStatus> statuses);

    Optional<Reservation> findFirstByBookIdAndStatusOrderByCreatedAtAsc(UUID bookId, ReservationStatus status);

    Optional<Reservation> findFirstByBookIdAndUserIdAndStatus(UUID bookId, UUID userId, ReservationStatus status);

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);

    Page<Reservation> findByUserId(UUID userId, Pageable pageable);
}
