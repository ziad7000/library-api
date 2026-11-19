package com.library.reservation.service;

import com.library.book.model.Book;
import com.library.book.repository.BookRepository;
import com.library.common.config.LibraryProperties;
import com.library.common.exception.BusinessRuleException;
import com.library.common.exception.ConflictException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.common.pagination.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final LoanRepository loanRepository;
    private final LibraryProperties properties;
    private final TimeProvider timeProvider;

    public ReservationService(
            ReservationRepository reservationRepository,
            BookRepository bookRepository,
            UserRepository userRepository,
            LoanRepository loanRepository,
            LibraryProperties properties,
            TimeProvider timeProvider
    ) {
        this.reservationRepository = reservationRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.loanRepository = loanRepository;
        this.properties = properties;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public ReservationResponse createReservation(UUID userId, ReservationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!user.isActive()) {
            throw new BusinessRuleException("Cannot place reservation: Account is deactivated");
        }

        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + request.bookId()));

        // Rule: Reservation only allowed when NO copies are available
        if (book.getAvailableCopies() > 0) {
            throw new BusinessRuleException("Cannot reserve: Copies of this book are currently available for immediate borrowing");
        }

        // Rule: Cannot reserve a book currently on active loan by user
        if (loanRepository.existsByBookIdAndUserIdAndStatus(book.getId(), userId, LoanStatus.ACTIVE)) {
            throw new ConflictException("You cannot reserve a book that you currently have on loan");
        }

        // Rule: Cannot reserve if member already has active reservation (WAITING or READY)
        if (reservationRepository.existsByBookIdAndUserIdAndStatusIn(
                book.getId(), userId, List.of(ReservationStatus.WAITING, ReservationStatus.READY))) {
            throw new ConflictException("You already have an active reservation for this book");
        }

        Reservation reservation = new Reservation();
        reservation.setBook(book);
        reservation.setUser(user);
        reservation.setStatus(ReservationStatus.WAITING);

        Reservation saved = reservationRepository.save(reservation);
        return ReservationResponse.from(saved);
    }

    @Transactional
    public void cancelReservation(UUID reservationId, UUID requestingUserId, Role requestingRole) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));

        if (requestingRole == Role.MEMBER && !reservation.getUser().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("You can only cancel your own reservations");
        }

        if (reservation.getStatus() != ReservationStatus.WAITING && reservation.getStatus() != ReservationStatus.READY) {
            throw new BusinessRuleException("Only WAITING or READY reservations can be cancelled");
        }

        boolean wasReady = (reservation.getStatus() == ReservationStatus.READY);
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        // If a copy was being held for this reservation, promote the next member or release copy
        if (wasReady) {
            promoteNextWaitingOrRelease(reservation.getBook().getId());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> getMyReservations(UUID userId, Pageable pageable) {
        Page<Reservation> page = reservationRepository.findByUserId(userId, pageable);
        return PageResponse.from(page.map(ReservationResponse::from));
    }

    @Transactional
    public void promoteNextWaitingOrRelease(UUID bookId) {
        Optional<Reservation> nextWaitingOpt = reservationRepository
                .findFirstByBookIdAndStatusOrderByCreatedAtAsc(bookId, ReservationStatus.WAITING);

        if (nextWaitingOpt.isPresent()) {
            Reservation next = nextWaitingOpt.get();
            Instant now = timeProvider.now();
            next.setStatus(ReservationStatus.READY);
            next.setReadyAt(now);
            next.setExpiresAt(now.plus(properties.holdDurationDays(), ChronoUnit.DAYS));
            reservationRepository.save(next);
            // Copy remains held: do NOT increment available copies
        } else {
            // Nobody waiting in queue: release copy back to general availability
            bookRepository.incrementAvailableCopies(bookId);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Reservation> findReadyReservation(UUID bookId, UUID userId) {
        return reservationRepository.findFirstByBookIdAndUserIdAndStatus(bookId, userId, ReservationStatus.READY);
    }

    @Transactional
    public void fulfillReservation(Reservation reservation) {
        reservation.setStatus(ReservationStatus.FULFILLED);
        reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public boolean hasPendingReservations(UUID bookId) {
        return reservationRepository.existsByBookIdAndStatusIn(
                bookId, List.of(ReservationStatus.WAITING, ReservationStatus.READY));
    }
}
