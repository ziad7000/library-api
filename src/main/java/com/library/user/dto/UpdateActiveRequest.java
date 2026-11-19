package com.library.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateActiveRequest(
        @NotNull(message = "Active status is required")
        Boolean active
) {}
