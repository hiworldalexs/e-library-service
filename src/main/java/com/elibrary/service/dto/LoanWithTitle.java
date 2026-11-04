package com.elibrary.service.dto;

import java.time.Instant;

public record LoanWithTitle(
        String bookId,
        String bookTitle,
        Instant borrowedAt,
        Instant dueDate
) {
}
