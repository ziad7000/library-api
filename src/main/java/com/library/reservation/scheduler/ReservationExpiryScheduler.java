package com.library.reservation.scheduler;

import com.library.common.time.TimeProvider;
import com.library.reservation.model.Reservation;
import com.library.reservation.model.ReservationStatus;
import com.library.reservation.repository.ReservationRepository;
import com.library.reservation.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class ReservationExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryScheduler.class);

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final TimeProvider timeProvider;

    public ReservationExpiryScheduler(
            ReservationRepository reservationRepository,
            ReservationService reservationService,
            TimeProvider timeProvider
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationService = reservationService;
        this.timeProvider = timeProvider;
    }

    @Scheduled(cron = "${library.reservation-expiry-cron:0 0 * * * *}") // Every hour by default
    @Transactional
    public void processExpiredReservations() {
        Instant now = timeProvider.now();
        List<Reservation> expiredReservations = reservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.READY, now);

        if (!expiredReservations.isEmpty()) {
            log.info("Processing {} expired library book reservations", expiredReservations.size());
        }

        for (Reservation reservation : expiredReservations) {
            log.info("Reservation {} for book {} expired at {}. Promoting next member or releasing hold.",
                    reservation.getId(), reservation.getBook().getTitle(), reservation.getExpiresAt());

            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);

            // Promote next WAITING member or increment available copies
            reservationService.promoteNextWaitingOrRelease(reservation.getBook().getId());
        }
    }
}
