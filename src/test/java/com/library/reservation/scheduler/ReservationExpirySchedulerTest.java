package com.library.reservation.scheduler;

import com.library.book.model.Book;
import com.library.book.model.Category;
import com.library.common.time.TimeProvider;
import com.library.reservation.model.Reservation;
import com.library.reservation.model.ReservationStatus;
import com.library.reservation.repository.ReservationRepository;
import com.library.reservation.service.ReservationService;
import com.library.user.model.Role;
import com.library.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationExpirySchedulerTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationService reservationService;

    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private ReservationExpiryScheduler scheduler;

    @Test
    @DisplayName("Scheduled job transitions expired READY reservations to EXPIRED and triggers promotion")
    void shouldProcessExpiredReservations() {
        Instant now = Instant.parse("2026-03-05T10:00:00Z");
        when(timeProvider.now()).thenReturn(now);

        Book book = new Book(UUID.randomUUID(), "9780134685991", "Title", "Desc", 2020, "English",
                new Category(UUID.randomUUID(), "Tech"), 1, 0);
        User user = new User(UUID.randomUUID(), "user@example.com", "hash", "User", Role.MEMBER, true);

        Reservation readyRes = new Reservation(UUID.randomUUID(), book, user);
        readyRes.setStatus(ReservationStatus.READY);
        readyRes.setExpiresAt(now.minusSeconds(3600)); // expired 1 hour ago

        when(reservationRepository.findByStatusAndExpiresAtBefore(ReservationStatus.READY, now))
                .thenReturn(List.of(readyRes));

        scheduler.processExpiredReservations();

        assertThat(readyRes.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        verify(reservationRepository).save(readyRes);
        verify(reservationService).promoteNextWaitingOrRelease(book.getId());
    }
}
