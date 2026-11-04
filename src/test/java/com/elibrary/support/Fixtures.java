package com.elibrary.support;

import com.elibrary.entity.Book;

import java.util.UUID;

public final class Fixtures {

    private Fixtures() {
    }

    public static Book.BookBuilder book() {
        return Book.builder()
                .id(UUID.randomUUID().toString())
                .title("Clean Code")
                .author("Author")
                .category("Tech")
                .totalCopies(1);
    }
}
