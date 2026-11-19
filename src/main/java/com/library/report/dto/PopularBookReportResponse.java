package com.library.report.dto;

import java.util.UUID;

public record PopularBookReportResponse(
        UUID bookId,
        String isbn,
        String title,
        long borrowCount
) {}
