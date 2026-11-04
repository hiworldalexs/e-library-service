package com.elibrary.service.dto;

import com.elibrary.entity.Book;
import com.elibrary.entity.Loan;

public record BorrowResult(Book book, Loan loan) {
}
