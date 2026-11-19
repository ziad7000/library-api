package com.library.book.repository;

import com.library.book.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID>, JpaSpecificationExecutor<Book> {

    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    @Query("SELECT DISTINCT b FROM Book b " +
            "LEFT JOIN b.authors a " +
            "LEFT JOIN b.category c " +
            "WHERE (:query IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND (:authorId IS NULL OR a.id = :authorId) " +
            "AND (:categoryId IS NULL OR c.id = :categoryId) " +
            "AND (:availableOnly IS FALSE OR b.availableCopies > 0)")
    Page<Book> search(
            @Param("query") String query,
            @Param("authorId") UUID authorId,
            @Param("categoryId") UUID categoryId,
            @Param("availableOnly") boolean availableOnly,
            Pageable pageable
    );

    @Modifying
    @Query("UPDATE Book b SET b.availableCopies = b.availableCopies - 1, b.updatedAt = CURRENT_TIMESTAMP WHERE b.id = :id AND b.availableCopies > 0")
    int decrementAvailableCopies(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Book b SET b.availableCopies = b.availableCopies + 1, b.updatedAt = CURRENT_TIMESTAMP WHERE b.id = :id AND b.availableCopies < b.totalCopies")
    int incrementAvailableCopies(@Param("id") UUID id);
}
