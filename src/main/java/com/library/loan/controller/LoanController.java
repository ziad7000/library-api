package com.library.loan.controller;

import com.library.auth.security.UserPrincipal;
import com.library.common.pagination.PageResponse;
import com.library.loan.dto.BorrowRequest;
import com.library.loan.dto.LoanResponse;
import com.library.loan.model.LoanStatus;
import com.library.loan.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@Tag(name = "Loans", description = "Book borrowing, renewal, returns, and concurrency management")
@SecurityRequirement(name = "Bearer Authentication")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    @Operation(summary = "Borrow a book", description = "Creates a new active loan for the authenticated member. Decrements availability atomically.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Book borrowed successfully",
                    content = @Content(schema = @Schema(implementation = LoanResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict: No copies available or duplicate active loan",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Business rule violated: Overdue loans, unpaid fines, or max limit reached",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<LoanResponse> borrowBook(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BorrowRequest request
    ) {
        LoanResponse response = loanService.borrowBook(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Return a borrowed book", description = "Marks loan as RETURNED and creates a fine if returned after due date. Restricted to LIBRARIAN and ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book returned successfully",
                    content = @Content(schema = @Schema(implementation = LoanResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Loan not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Loan already returned",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<LoanResponse> returnBook(@PathVariable UUID id) {
        LoanResponse response = loanService.returnBook(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/renew")
    @Operation(summary = "Renew a loan", description = "Extends the due date by 14 days if within limits (max 2 renewals, not overdue).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan renewed successfully",
                    content = @Content(schema = @Schema(implementation = LoanResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden: Not your loan",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Loan not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Business rule violated: Maximum renewals reached or loan overdue",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<LoanResponse> renewLoan(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        LoanResponse response = loanService.renewLoan(id, principal.getId(), principal.getRole());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get authenticated member's loans", description = "Returns loans of the currently logged-in user with optional status filter.")
    public ResponseEntity<PageResponse<LoanResponse>> getMyLoans(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) LoanStatus status,
            @PageableDefault(sort = "borrowedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(loanService.getMyLoans(principal.getId(), status, pageable));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Search all loans", description = "Administrative query of all loans with user, status, and overdue filtering. Restricted to LIBRARIAN and ADMIN.")
    public ResponseEntity<PageResponse<LoanResponse>> searchLoans(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(defaultValue = "false") boolean overdue,
            @PageableDefault(sort = "borrowedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(loanService.searchLoans(userId, status, overdue, pageable));
    }
}
