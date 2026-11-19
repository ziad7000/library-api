package com.library.fine.service;

import com.library.common.exception.BusinessRuleException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.common.pagination.PageResponse;
import com.library.common.time.TimeProvider;
import com.library.fine.dto.FineResponse;
import com.library.fine.model.Fine;
import com.library.fine.model.FineStatus;
import com.library.fine.repository.FineRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FineService {

    private final FineRepository fineRepository;
    private final TimeProvider timeProvider;

    public FineService(FineRepository fineRepository, TimeProvider timeProvider) {
        this.fineRepository = fineRepository;
        this.timeProvider = timeProvider;
    }

    @Transactional(readOnly = true)
    public PageResponse<FineResponse> getMyFines(UUID userId, Pageable pageable) {
        Page<Fine> page = fineRepository.findByUserId(userId, pageable);
        return PageResponse.from(page.map(FineResponse::from));
    }

    @Transactional
    public FineResponse payFine(UUID fineId) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine not found with id: " + fineId));

        if (fine.getStatus() == FineStatus.PAID) {
            throw new BusinessRuleException("Fine has already been marked as paid");
        }

        fine.setStatus(FineStatus.PAID);
        fine.setPaidAt(timeProvider.now());

        Fine saved = fineRepository.save(fine);
        return FineResponse.from(saved);
    }
}
