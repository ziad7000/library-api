package com.library.book.dto;

import com.library.book.model.Category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
