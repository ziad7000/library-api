package com.library.report.service;

import com.library.book.model.Book;
import com.library.book.model.Category;
import com.library.common.time.TimeProvider;
import com.library.loan.model.Loan;
import com.library.loan.repository.LoanRepository;
import com.library.report.dto.OverdueReportResponse;
import com.library.report.dto.PopularBookReportResponse;
import com.library.user.model.Role;
import com.library.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private ReportService reportService;

    @Test
    @DisplayName("Overdue report calculates days late correctly for each overdue loan")
    void shouldGenerateOverdueReportWithAccurateDaysLate() {
        LocalDate today = LocalDate.of(2026, 3, 10);
        when(timeProvider.today()).thenReturn(today);

        Book book = new Book(UUID.randomUUID(), "9780134685991", "Effective Java", "Desc",
                2018, "English", new Category(UUID.randomUUID(), "Tech"), 5, 4);
        User user = new User(UUID.randomUUID(), "borrower@example.com", "hash", "Borrower", Role.MEMBER, true);

        // Due date was 2026-03-03 (7 days late)
        LocalDate dueDate = LocalDate.of(2026, 3, 3);
        Loan loan = new Loan(UUID.randomUUID(), book, user, Instant.now(), dueDate);

        when(loanRepository.findOverdueLoansWithDetails(today)).thenReturn(List.of(loan));

        List<OverdueReportResponse> report = reportService.getOverdueLoansReport();

        assertThat(report).hasSize(1);
        OverdueReportResponse item = report.get(0);
        assertThat(item.bookTitle()).isEqualTo("Effective Java");
        assertThat(item.userEmail()).isEqualTo("borrower@example.com");
        assertThat(item.daysOverdue()).isEqualTo(7);
    }

    @Test
    @DisplayName("Popular books report passes correct date window and limit to repository")
    void shouldQueryPopularBooksWithCorrectWindow() {
        Instant now = Instant.parse("2026-03-10T12:00:00Z");
        when(timeProvider.now()).thenReturn(now);

        PopularBookReportResponse bookStat = new PopularBookReportResponse(
                UUID.randomUUID(), "9780134685991", "Effective Java", 42L);

        when(loanRepository.findMostBorrowedBooksSince(any(Instant.class), eq(PageRequest.of(0, 10))))
                .thenReturn(List.of(bookStat));

        List<PopularBookReportResponse> report = reportService.getPopularBooksReport(30, 10);

        assertThat(report).hasSize(1);
        assertThat(report.get(0).borrowCount()).isEqualTo(42L);
    }
}
