package com.library.fine.repository;

import com.library.fine.model.Fine;
import com.library.fine.model.FineStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FineRepository extends JpaRepository<Fine, UUID> {

    List<Fine> findByUserIdAndStatus(UUID userId, FineStatus status);

    boolean existsByUserIdAndStatus(UUID userId, FineStatus status);

    Page<Fine> findByUserId(UUID userId, Pageable pageable);
}
