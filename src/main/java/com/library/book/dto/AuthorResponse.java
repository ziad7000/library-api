package com.library.book.dto;

import com.library.book.model.Author;

import java.util.UUID;

public record AuthorResponse(
        UUID id,
        String name
) {
    public static AuthorResponse from(Author author) {
        return new AuthorResponse(author.getId(), author.getName());
    }
}
