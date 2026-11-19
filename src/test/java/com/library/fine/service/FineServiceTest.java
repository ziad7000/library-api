package com.library.fine.service;

import com.library.book.model.Book;
import com.library.book.model.Category;
import com.library.common.exception.BusinessRuleException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.common.time.TimeProvider;
import com.library.fine.dto.FineResponse;
import com.library.fine.model.Fine;
import com.library.fine.model.FineStatus;
import com.library.fine.repository.FineRepository;
import com.library.loan.model.Loan;
import com.library.user.model.Role;
import com.library.user.model.User;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FineServiceTest {

    @Mock
    private FineRepository fineRepository;

    @Mock
    private TimeProvider timeProvider;

    @InjectMocks
    private FineService fineService;

    @Test
    @DisplayName("Pay fine successfully transitions fine status to PAID and sets paidAt timestamp")
    void shouldPayFineSuccessfully() {
        UUID fineId = UUID.randomUUID();
        Instant now = Instant.parse("2026-03-01T14:00:00Z");

        User user = new User(UUID.randomUUID(), "user@example.com", "hash", "User", Role.MEMBER, true);
        Book book = new Book(UUID.randomUUID(), "9780134685991", "Title", "Desc", 2020, "English",
                new Category(UUID.randomUUID(), "Tech"), 1, 1);
        Loan loan = new Loan(UUID.randomUUID(), book, user, now.minusSeconds(86400 * 10), LocalDate.now().minusDays(5));
        Fine fine = new Fine(fineId, loan, user, new BigDecimal("10.00"), FineStatus.UNPAID);

        when(fineRepository.findById(fineId)).thenReturn(Optional.of(fine));
        when(timeProvider.now()).thenReturn(now);
        when(fineRepository.save(any(Fine.class))).thenReturn(fine);

        FineResponse response = fineService.payFine(fineId);

        assertThat(response.status()).isEqualTo(FineStatus.PAID);
        assertThat(fine.getStatus()).isEqualTo(FineStatus.PAID);
        assertThat(fine.getPaidAt()).isEqualTo(now);
        verify(fineRepository).save(fine);
    }

    @Test
    @DisplayName("Pay fine throws BusinessRuleException when fine has already been paid")
    void shouldThrowWhenFineAlreadyPaid() {
        UUID fineId = UUID.randomUUID();
        User user = new User(UUID.randomUUID(), "user@example.com", "hash", "User", Role.MEMBER, true);
        Book book = new Book(UUID.randomUUID(), "9780134685991", "Title", "Desc", 2020, "English",
                new Category(UUID.randomUUID(), "Tech"), 1, 1);
        Loan loan = new Loan(UUID.randomUUID(), book, user, Instant.now(), LocalDate.now());
        Fine fine = new Fine(fineId, loan, user, new BigDecimal("10.00"), FineStatus.PAID);

        when(fineRepository.findById(fineId)).thenReturn(Optional.of(fine));

        assertThatThrownBy(() -> fineService.payFine(fineId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already been marked as paid");
    }

    @Test
    @DisplayName("Pay fine throws ResourceNotFoundException when fine does not exist")
    void shouldThrowWhenFineNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(fineRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fineService.payFine(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
