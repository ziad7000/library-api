package com.library.report.controller;

import com.library.report.dto.OverdueReportResponse;
import com.library.report.dto.PopularBookReportResponse;
import com.library.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Administrative circulation analytics, overdue monitoring, and popular books")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue loans report", description = "Lists all active loans whose due date has elapsed with borrower info and days late. Restricted to LIBRARIAN and ADMIN.")
    public ResponseEntity<List<OverdueReportResponse>> getOverdueLoansReport() {
        return ResponseEntity.ok(reportService.getOverdueLoansReport());
    }

    @GetMapping("/popular-books")
    @Operation(summary = "Get most borrowed books report", description = "Lists the most borrowed books over the specified time window (default 30 days). Restricted to LIBRARIAN and ADMIN.")
    public ResponseEntity<List<PopularBookReportResponse>> getPopularBooksReport(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(reportService.getPopularBooksReport(days, limit));
    }
}
