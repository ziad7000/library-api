package com.library.loan.service;

import com.library.book.model.Book;
import com.library.book.model.Category;
import com.library.book.repository.BookRepository;
import com.library.common.config.LibraryProperties;
import com.library.common.exception.BusinessRuleException;
import com.library.common.exception.ConflictException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FineRepository fineRepository;

    @Mock
    private LibraryProperties properties;

    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private LoanService loanService;

    private final LocalDate today = LocalDate.of(2026, 3, 1);
    private final Instant now = Instant.parse("2026-03-01T10:00:00Z");

    @BeforeEach
    void setUp() {
        when(timeProvider.today()).thenReturn(today);
    }

    @Test
    @DisplayName("Borrow succeeds when member is eligible and book copy is available")
    void shouldBorrowSuccessfully() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        BorrowRequest request = new BorrowRequest(bookId);

        User user = new User(userId, "user@example.com", "hash", "User", Role.MEMBER, true);
        Book book = new Book(bookId, "9780134685991", "Title", "Desc", 2020, "English",
                new Category(UUID.randomUUID(), "Tech"), 5, 5);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(properties.maxActiveLoans()).thenReturn(5);
        when(loanRepository.countByUserIdAndStatus(userId, LoanStatus.ACTIVE)).thenReturn(2L);
        when(loanRepository.existsByBookIdAndUserIdAndStatus(bookId, userId, LoanStatus.ACTIVE)).thenReturn(false);
        when(loanRepository.existsByUserIdAndStatusAndDueDateBefore(userId, LoanStatus.ACTIVE, today)).thenReturn(false);
        when(fineRepository.existsByUserIdAndStatus(userId, FineStatus.UNPAID)).thenReturn(false);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookRepository.decrementAvailableCopies(bookId)).thenReturn(1);
        when(properties.loanPeriodDays()).thenReturn(14);

        Loan savedLoan = new Loan(UUID.randomUUID(), book, user, now, today.plusDays(14));
        when(loanRepository.save(any(Loan.class))).thenReturn(savedLoan);

        LoanResponse response = loanService.borrowBook(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.dueDate()).isEqualTo(today.plusDays(14));
        assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);
        verify(bookRepository).decrementAvailableCopies(bookId);
    }

    @Test
    @DisplayName("Borrow throws BusinessRuleException when active loans limit is reached")
    void shouldBlockWhenMaxLoansReached() {
        UUID userId = UUID.randomUUID();
        BorrowRequest request = new BorrowRequest(UUID.randomUUID());
        User user = new User(userId, "user@example.com", "hash", "User", Role.MEMBER, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(properties.maxActiveLoans()).thenReturn(5);
        when(loanRepository.countByUserIdAndStatus(userId, LoanStatus.ACTIVE)).thenReturn(5L);

        assertThatThrownBy(() -> loanService.borrowBook(userId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("limit reached");
    }

    @Test
    @DisplayName("Borrow throws ConflictException when user already holds active loan of same book")
    void shouldBlockDuplicateActiveBookLoan() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        BorrowRequest request = new BorrowRequest(bookId);
        User user = new User(userId, "user@example.com", "hash", "User", Role.MEMBER, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(properties.maxActiveLoans()).thenReturn(5);
        when(loanRepository.countByUserIdAndStatus(userId, LoanStatus.ACTIVE)).thenReturn(1L);
        when(loanRepository.existsByBookIdAndUserIdAndStatus(bookId, userId, LoanStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> loanService.borrowBook(userId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already have an active loan for this book");
    }

    @Test
    @DisplayName("Borrow throws BusinessRuleException when member has overdue loans")
    void shouldBlockWhenOverdueLoansPresent() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        BorrowRequest request = new BorrowRequest(bookId);
        User user = new User(userId, "user@example.com", "hash", "User", Role.MEMBER, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(properties.maxActiveLoans()).thenReturn(5);
        when(loanRepository.countByUserIdAndStatus(userId, LoanStatus.ACTIVE)).thenReturn(1L);
        when(loanRepository.existsByBookIdAndUserIdAndStatus(bookId, userId, LoanStatus.ACTIVE)).thenReturn(false);
        when(loanRepository.existsByUserIdAndStatusAndDueDateBefore(userId, LoanStatus.ACTIVE, today)).thenReturn(true);

        assertThatThrownBy(() -> loanService.borrowBook(userId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("overdue active loans");
    }

    @Test
    @DisplayName("Borrow throws BusinessRuleException when member has unpaid fines")
    void shouldBlockWhenUnpaidFinesPresent() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        BorrowRequest request = new BorrowRequest(bookId);
        User user = new User(userId, "user@example.com", "hash", "User", Role.MEMBER, true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(properties.maxActiveLoans()).thenReturn(5);
        when(loanRepository.countByUserIdAndStatus(userId, LoanStatus.ACTIVE)).thenReturn(1L);
        when(loanRepository.existsByBookIdAndUserIdAndStatus(bookId, userId, LoanStatus.ACTIVE)).thenReturn(false);
        when(loanRepository.existsByUserIdAndStatusAndDueDateBefore(userId, LoanStatus.ACTIVE, today)).thenReturn(false);
        when(fineRepository.existsByUserIdAndStatus(userId, FineStatus.UNPAID)).thenReturn(true);

        assertThatThrownBy(() -> loanService.borrowBook(userId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("unpaid fines");
    }

    @Test
    @DisplayName("Borrow throws ConflictException when atomic decrement fails (0 rows updated)")
    void shouldThrowConflictWhenNoCopiesAvailable() {
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        BorrowRequest request = new BorrowRequest(bookId);
        User user = new User(userId, "user@example.com", "hash", "User", Role.MEMBER, true);
        Book book = new Book(bookId, "9780134685991", "Title", "Desc", 2020, "English",
                new Category(UUID.randomUUID(), "Tech"), 1, 0);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(properties.maxActiveLoans()).thenReturn(5);
        when(loanRepository.countByUserIdAndStatus(userId, LoanStatus.ACTIVE)).thenReturn(0L);
        when(loanRepository.existsByBookIdAndUserIdAndStatus(bookId, userId, LoanStatus.ACTIVE)).thenReturn(false);
        when(loanRepository.existsByUserIdAndStatusAndDueDateBefore(userId, LoanStatus.ACTIVE, today)).thenReturn(false);
        when(fineRepository.existsByUserIdAndStatus(userId, FineStatus.UNPAID)).thenReturn(false);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookRepository.decrementAvailableCopies(bookId)).thenReturn(0); // 0 copies decremented!

        assertThatThrownBy(() -> loanService.borrowBook(userId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("No copies available");
    }

    @Test
    @DisplayName("Return creates a fine if returned after due date")
    void shouldCreateFineWhenReturnedLate() {
        UUID loanId = UUID.randomUUID();
        Book book = new Book(UUID.randomUUID(), "9780134685991", "Title", "Desc", 2020, "English",
                new Category(UUID.randomUUID(), "Tech"), 1, 0);
        User user = new User(UUID.randomUUID(), "user@example.com", "hash", "User", Role.MEMBER, true);

        // Due date was 5 days ago (2026-02-24)
        LocalDate pastDueDate = today.minusDays(5);
        Loan loan = new Loan(loanId, book, user, now.minusSeconds(86400 * 20), pastDueDate);

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(properties.dailyFineRate()).thenReturn(new BigDecimal("2.00"));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        loanService.returnBook(loanId);

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.RETURNED);
        // 5 days * 2.00 = 10.00
        verify(fineRepository).save(any(Fine.class));
        verify(bookRepository).incrementAvailableCopies(book.getId());
    }

    @Test
    @DisplayName("Return on time does not create any fine")
    void shouldNotCreateFineWhenReturnedOnTime() {
        UUID loanId = UUID.randomUUID();
        Book book = new Book(UUID.randomUUID(), "9780134685991", "Title", "Desc", 2020, "English",
                new Category(UUID.randomUUID(), "Tech"), 1, 0);
        User user = new User(UUID.randomUUID(), "user@example.com", "hash", "User", Role.MEMBER, true);

        // Due date is in 7 days
        LocalDate futureDueDate = today.plusDays(7);
        Loan loan = new Loan(loanId, book, user, now, futureDueDate);

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        loanService.returnBook(loanId);

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.RETURNED);
        verify(fineRepository, never()).save(any(Fine.class));
        verify(bookRepository).incrementAvailableCopies(book.getId());
    }

    @Test
    @DisplayName("Renew extends due date by 14 days and increments renewalsCount")
    void shouldRenewLoanSuccessfully() {
        UUID loanId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Book book = new Book(UUID.randomUUID(), "9780134685991", "Title", "Desc", 2020, "English",
                new Category(UUID.randomUUID(), "Tech"), 1, 0);
        User user = new User(userId, "user@example.com", "hash", "User", Role.MEMBER, true);

        LocalDate currentDueDate = today.plusDays(5);
        Loan loan = new Loan(loanId, book, user, now, currentDueDate);
        loan.setRenewalsCount(0);

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(properties.maxRenewals()).thenReturn(2);
        when(properties.loanPeriodDays()).thenReturn(14);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        LoanResponse response = loanService.renewLoan(loanId, userId, Role.MEMBER);

        assertThat(response.renewalsCount()).isEqualTo(1);
        assertThat(loan.getDueDate()).isEqualTo(currentDueDate.plusDays(14));
    }

    @Test
    @DisplayName("Renew throws BusinessRuleException when renewal limit (2) is reached")
    void shouldBlockRenewWhenLimitReached() {
        UUID loanId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "user@example.com", "hash", "User", Role.MEMBER, true);
        Book book = new Book(UUID.randomUUID(), "9780134685991", "Title", "Desc", 2020, "English",
                new Category(UUID.randomUUID(), "Tech"), 1, 0);

        Loan loan = new Loan(loanId, book, user, now, today.plusDays(5));
        loan.setRenewalsCount(2);

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(properties.maxRenewals()).thenReturn(2);

        assertThatThrownBy(() -> loanService.renewLoan(loanId, userId, Role.MEMBER))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("limit");
    }
}
