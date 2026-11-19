package com.library.loan.repository;

import com.library.loan.model.Loan;
import com.library.loan.model.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    long countByUserIdAndStatus(UUID userId, LoanStatus status);

    boolean existsByBookIdAndUserIdAndStatus(UUID bookId, UUID userId, LoanStatus status);

    boolean existsByUserIdAndStatusAndDueDateBefore(UUID userId, LoanStatus status, LocalDate date);

    Page<Loan> findByUserIdAndStatus(UUID userId, LoanStatus status, Pageable pageable);

    Page<Loan> findByUserId(UUID userId, Pageable pageable);

    @Query("SELECT l FROM Loan l " +
            "WHERE (:userId IS NULL OR l.user.id = :userId) " +
            "AND (:status IS NULL OR l.status = :status) " +
            "AND (:overdue IS FALSE OR (l.status = 'ACTIVE' AND l.dueDate < :today))")
    Page<Loan> searchLoans(
            @Param("userId") UUID userId,
            @Param("status") LoanStatus status,
            @Param("overdue") boolean overdue,
            @Param("today") LocalDate today,
            Pageable pageable
    );

    @Query("SELECT l FROM Loan l " +
            "JOIN FETCH l.book b " +
            "JOIN FETCH l.user u " +
            "WHERE l.status = 'ACTIVE' AND l.dueDate < :today " +
            "ORDER BY l.dueDate ASC")
    List<Loan> findOverdueLoansWithDetails(@Param("today") LocalDate today);
}
