package com.library.book.dto;

import com.library.book.model.Book;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookResponse(
        UUID id,
        String isbn,
        String title,
        String description,
        int publicationYear,
        String language,
        CategoryResponse category,
        List<AuthorResponse> authors,
        int totalCopies,
        int availableCopies,
        Instant createdAt
) {
    public static BookResponse from(Book book) {
        return new BookResponse(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getDescription(),
                book.getPublicationYear(),
                book.getLanguage(),
                book.getCategory() != null ? CategoryResponse.from(book.getCategory()) : null,
                book.getAuthors() != null ? book.getAuthors().stream().map(AuthorResponse::from).toList() : List.of(),
                book.getTotalCopies(),
                book.getAvailableCopies(),
                book.getCreatedAt()
        );
    }
}
