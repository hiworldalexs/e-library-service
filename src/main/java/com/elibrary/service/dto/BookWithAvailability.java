package com.elibrary.service.dto;

import com.elibrary.entity.Book;

public record BookWithAvailability(Book book, int availableCopies) {
}
