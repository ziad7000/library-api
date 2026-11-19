package com.library.book.dto;

import com.library.book.model.Author;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AuthorRequest(
        @NotBlank(message = "Author name is required")
        @Size(min = 2, max = 255, message = "Author name must be between 2 and 255 characters")
        String name
) {}
