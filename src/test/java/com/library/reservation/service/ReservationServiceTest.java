package com.library.reservation.service;

import com.library.book.model.Book;
import com.library.book.model.Category;
import com.library.book.repository.BookRepository;
import com.library.common.config.LibraryProperties;
import com.library.common.exception.BusinessRuleException;
import com.library.common.exception.ConflictException;
import com.library.common.time.TimeProvider;
import com.library.loan.model.LoanStatus;
import com.library.loan.repository.LoanRepository;
import com.library.reservation.dto.ReservationRequest;
import com.library.reservation.dto.ReservationResponse;
import com.library.reservation.model.Reservation;
import com.library.reservation.model.ReservationStatus;
import com.library.reservation.repository.ReservationRepository;
import com.library.user.model.Role;
import com.library.user.model.User;
import com.library.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LibraryProperties properties;

    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    @DisplayName("Successfully creates WAITING reservation when no copies are available")
    void shouldCreateReservationWhenNoCopiesAvailable() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        ReservationRequest request = new ReservationRequest(bookId);

        User user = new User(userId, "user@example.com", "hash", "User", Role.MEMBER, true);
        Book book = new Book(bookId, "9780134685991", "Title", "Desc", 2020, "English",
                new Category(UUID.randomUUID(), "Tech"), 3, 0); // 0 copies available

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(loanRepository.existsByBookIdAndUserIdAndStatus(bookId, userId, LoanStatus.ACTIVE)).thenReturn(false);
        when(reservationRepository.existsByBookIdAndUserIdAndStatusIn(eq(bookId), eq(userId), any())).thenReturn(false);

        Reservation savedReservation = new Reservation(UUID.randomUUID(), book, user);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        ReservationResponse response = reservationService.createReservation(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(ReservationStatus.WAITING);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("Throws BusinessRuleException when copies are available for immediate borrowing")
    void shouldBlockReservationWhenCopiesAvailable() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        ReservationRequest request = new ReservationRequest(bookId);

        User user = new User(userId, "user@example.com", "hash", "User", Role.MEMBER, true);
        Book book = new Book(bookId, "9780134685991", "Title", "Desc", 2020, "English",
                new Category(UUID.randomUUID(), "Tech"), 3, 1); // 1 copy available!

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> reservationService.createReservation(userId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("available for immediate borrowing");
    }

    @Test
    @DisplayName("Throws ConflictException when member already holds active loan of same book")
    void shouldBlockReservationWhenActiveLoanExists() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        ReservationRequest request = new ReservationRequest(bookId);

        User user = new User(userId, "user@example.com", "hash", "User", Role.MEMBER, true);
        Book book = new Book(bookId, "9780134685991", "Title", "Desc", 2020, "English",
                new Category(UUID.randomUUID(), "Tech"), 3, 0);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(loanRepository.existsByBookIdAndUserIdAndStatus(bookId, userId, LoanStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> reservationService.createReservation(userId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("currently have on loan");
    }

    @Test
    @DisplayName("Promotes oldest WAITING reservation to READY and holds copy without incrementing available_copies")
    void shouldPromoteOldestWaitingReservation() {
        UUID bookId = UUID.randomUUID();
        Book book = new Book(bookId, "9780134685991", "Title", "Desc", 2020, "English",
                new Category(UUID.randomUUID(), "Tech"), 3, 0);
        User user = new User(UUID.randomUUID(), "user@example.com", "hash", "User", Role.MEMBER, true);

        Reservation waiting = new Reservation(UUID.randomUUID(), book, user);
        when(reservationRepository.findFirstByBookIdAndStatusOrderByCreatedAtAsc(bookId, ReservationStatus.WAITING))
                .thenReturn(Optional.of(waiting));
        when(timeProvider.now()).thenReturn(Instant.parse("2026-03-01T12:00:00Z"));
        when(properties.holdDurationDays()).thenReturn(3);

        reservationService.promoteNextWaitingOrRelease(bookId);

        assertThat(waiting.getStatus()).isEqualTo(ReservationStatus.READY);
        assertThat(waiting.getReadyAt()).isNotNull();
        assertThat(waiting.getExpiresAt()).isEqualTo(Instant.parse("2026-03-04T12:00:00Z"));
        verify(bookRepository, never()).incrementAvailableCopies(bookId); // copy is held!
    }

    @Test
    @DisplayName("Releases copy back to general availability when reservation queue is empty")
    void shouldReleaseCopyWhenNoReservationsWaiting() {
        UUID bookId = UUID.randomUUID();
        when(reservationRepository.findFirstByBookIdAndStatusOrderByCreatedAtAsc(bookId, ReservationStatus.WAITING))
                .thenReturn(Optional.empty());

        reservationService.promoteNextWaitingOrRelease(bookId);

        verify(bookRepository).incrementAvailableCopies(bookId);
    }
}
