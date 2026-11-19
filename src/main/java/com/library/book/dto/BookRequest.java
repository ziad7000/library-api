package com.library.book.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record BookRequest(
        @NotBlank(message = "ISBN is required")
        @Pattern(regexp = "^(978|979)[0-9]{10}$", message = "ISBN must be a valid 13-digit ISBN starting with 978 or 979")
        String isbn,

        @NotBlank(message = "Title is required")
        @Size(min = 1, max = 500, message = "Title must be between 1 and 500 characters")
        String title,

        String description,

        @Min(value = 1400, message = "Publication year must be 1400 or later")
        @Max(value = 2100, message = "Publication year cannot be in the distant future")
        int publicationYear,

        @NotBlank(message = "Language is required")
        @Size(min = 2, max = 50, message = "Language must be between 2 and 50 characters")
        String language,

        @NotNull(message = "Category ID is required")
        UUID categoryId,

        @NotEmpty(message = "At least one author is required")
        List<UUID> authorIds,

        @Positive(message = "Total copies must be greater than zero")
        int totalCopies
) {}
