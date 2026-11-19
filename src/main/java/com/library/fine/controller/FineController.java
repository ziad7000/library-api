package com.library.fine.controller;

import com.library.auth.security.UserPrincipal;
import com.library.common.pagination.PageResponse;
import com.library.fine.dto.FineResponse;
import com.library.fine.service.FineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fines")
@Tag(name = "Fines", description = "Overdue return penalty calculation and simulated payment processing")
@SecurityRequirement(name = "Bearer Authentication")
public class FineController {

    private final FineService fineService;

    public FineController(FineService fineService) {
        this.fineService = fineService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current member's fines", description = "Returns all overdue fines assessed to the logged-in member.")
    public ResponseEntity<PageResponse<FineResponse>> getMyFines(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(fineService.getMyFines(principal.getId(), pageable));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    @Operation(summary = "Mark fine as paid", description = "Simulates settlement of an overdue penalty. Restricted to LIBRARIAN and ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fine marked as paid",
                    content = @Content(schema = @Schema(implementation = FineResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Fine not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Fine already paid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<FineResponse> payFine(@PathVariable UUID id) {
        FineResponse response = fineService.payFine(id);
        return ResponseEntity.ok(response);
    }
}
