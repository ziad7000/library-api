package com.library.report.service;

import com.library.common.time.TimeProvider;
import com.library.loan.model.Loan;
import com.library.loan.repository.LoanRepository;
import com.library.report.dto.OverdueReportResponse;
import com.library.report.dto.PopularBookReportResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReportService {

    private final LoanRepository loanRepository;
    private final TimeProvider timeProvider;

    public ReportService(LoanRepository loanRepository, TimeProvider timeProvider) {
        this.loanRepository = loanRepository;
        this.timeProvider = timeProvider;
    }

    @Transactional(readOnly = true)
    public List<OverdueReportResponse> getOverdueLoansReport() {
        LocalDate today = timeProvider.today();
        List<Loan> overdueLoans = loanRepository.findOverdueLoansWithDetails(today);

        return overdueLoans.stream()
                .map(loan -> {
                    long daysLate = ChronoUnit.DAYS.between(loan.getDueDate(), today);
                    return new OverdueReportResponse(
                            loan.getId(),
                            loan.getBook().getId(),
                            loan.getBook().getTitle(),
                            loan.getBook().getIsbn(),
                            loan.getUser().getId(),
                            loan.getUser().getEmail(),
                            loan.getUser().getFullName(),
                            loan.getDueDate(),
                            daysLate
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PopularBookReportResponse> getPopularBooksReport(int days, int limit) {
        if (days <= 0) {
            days = 30;
        }
        if (limit <= 0 || limit > 100) {
            limit = 10;
        }

        var since = timeProvider.now().minus(days, ChronoUnit.DAYS);
        return loanRepository.findMostBorrowedBooksSince(since, PageRequest.of(0, limit));
    }
}
