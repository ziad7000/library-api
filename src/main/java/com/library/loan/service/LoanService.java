package com.library.loan.service;

import com.library.book.model.Book;
import com.library.book.repository.BookRepository;
import com.library.common.config.LibraryProperties;
import com.library.common.exception.BusinessRuleException;
import com.library.common.exception.ConflictException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.common.pagination.PageResponse;
import com.library.common.time.TimeProvider;
import com.library.fine.model.Fine;
import com.library.fine.model.FineStatus;
import com.library.fine.repository.FineRepository;
import com.library.loan.dto.BorrowRequest;
import com.library.loan.dto.LoanResponse;
import com.library.loan.model.Loan;
import com.library.loan.model.LoanStatus;
import com.library.loan.repository.LoanRepository;
import com.library.user.model.Role;
import com.library.user.model.User;
import com.library.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final FineRepository fineRepository;
    private final LibraryProperties properties;
    private final TimeProvider timeProvider;

    public LoanService(
            LoanRepository loanRepository,
            BookRepository bookRepository,
            UserRepository userRepository,
            FineRepository fineRepository,
            LibraryProperties properties,
            TimeProvider timeProvider
    ) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.fineRepository = fineRepository;
        this.properties = properties;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public LoanResponse borrowBook(UUID userId, BorrowRequest request) {
        LocalDate today = timeProvider.today();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!user.isActive()) {
            throw new BusinessRuleException("Cannot borrow: Account is deactivated");
        }

        // Rule: Maximum active loans check
        long activeLoans = loanRepository.countByUserIdAndStatus(userId, LoanStatus.ACTIVE);
        if (activeLoans >= properties.maxActiveLoans()) {
            throw new BusinessRuleException("Maximum active loan limit reached (" + properties.maxActiveLoans() + ")");
        }

        // Rule: Cannot hold two active loans of the same book
        if (loanRepository.existsByBookIdAndUserIdAndStatus(request.bookId(), userId, LoanStatus.ACTIVE)) {
            throw new ConflictException("You already have an active loan for this book");
        }

        // Rule: Cannot borrow if user has ANY overdue active loans
        if (loanRepository.existsByUserIdAndStatusAndDueDateBefore(userId, LoanStatus.ACTIVE, today)) {
            throw new BusinessRuleException("Cannot borrow: You have overdue active loans. Please return them first.");
        }

        // Rule: Cannot borrow if user has ANY unpaid fines
        if (fineRepository.existsByUserIdAndStatus(userId, FineStatus.UNPAID)) {
            throw new BusinessRuleException("Cannot borrow: You have outstanding unpaid fines. Please settle them first.");
        }

        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + request.bookId()));

        // Atomic decrement for concurrency safety
        int updatedRows = bookRepository.decrementAvailableCopies(book.getId());
        if (updatedRows == 0) {
            throw new ConflictException("No copies available for borrowing for book: " + book.getTitle());
        }

        Loan loan = new Loan();
        loan.setBook(book);
        loan.setUser(user);
        loan.setBorrowedAt(timeProvider.now());
        loan.setDueDate(today.plusDays(properties.loanPeriodDays()));
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setRenewalsCount(0);

        Loan saved = loanRepository.save(loan);
        return LoanResponse.from(saved, today);
    }

    @Transactional
    public LoanResponse returnBook(UUID loanId) {
        LocalDate today = timeProvider.today();

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessRuleException("Loan has already been marked as returned");
        }

        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnedAt(timeProvider.now());

        // Check if returned late -> create fine
        if (today.isAfter(loan.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(loan.getDueDate(), today);
            BigDecimal fineAmount = properties.dailyFineRate().multiply(BigDecimal.valueOf(overdueDays));

            Fine fine = new Fine();
            fine.setLoan(loan);
            fine.setUser(loan.getUser());
            fine.setAmount(fineAmount);
            fine.setStatus(FineStatus.UNPAID);
            fineRepository.save(fine);
        }

        // Increase available copies back
        bookRepository.incrementAvailableCopies(loan.getBook().getId());

        Loan saved = loanRepository.save(loan);
        return LoanResponse.from(saved, today);
    }

    @Transactional
    public LoanResponse renewLoan(UUID loanId, UUID requestingUserId, Role requestingRole) {
        LocalDate today = timeProvider.today();

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        // Ownership validation
        if (requestingRole == Role.MEMBER && !loan.getUser().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("You can only renew your own loans");
        }

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new BusinessRuleException("Only active loans can be renewed");
        }

        if (loan.getRenewalsCount() >= properties.maxRenewals()) {
            throw new BusinessRuleException("Maximum renewals limit of " + properties.maxRenewals() + " reached");
        }

        if (today.isAfter(loan.getDueDate())) {
            throw new BusinessRuleException("Cannot renew an overdue loan. Please return the book.");
        }

        loan.setDueDate(loan.getDueDate().plusDays(properties.loanPeriodDays()));
        loan.setRenewalsCount(loan.getRenewalsCount() + 1);

        Loan saved = loanRepository.save(loan);
        return LoanResponse.from(saved, today);
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> getMyLoans(UUID userId, LoanStatus status, Pageable pageable) {
        LocalDate today = timeProvider.today();
        Page<Loan> page = (status != null)
                ? loanRepository.findByUserIdAndStatus(userId, status, pageable)
                : loanRepository.findByUserId(userId, pageable);

        return PageResponse.from(page.map(l -> LoanResponse.from(l, today)));
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> searchLoans(UUID userId, LoanStatus status, boolean overdue, Pageable pageable) {
        LocalDate today = timeProvider.today();
        Page<Loan> page = loanRepository.searchLoans(userId, status, overdue, today, pageable);
        return PageResponse.from(page.map(l -> LoanResponse.from(l, today)));
    }
}
