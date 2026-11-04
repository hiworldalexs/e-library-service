package com.elibrary.integration;

import com.elibrary.entity.Book;
import com.elibrary.exception.ELibraryException;
import com.elibrary.exception.ErrorCode;
import com.elibrary.repository.BookRepository;
import com.elibrary.repository.LoanRepository;
import com.elibrary.service.LibraryService;
import com.elibrary.service.dto.BorrowResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LibraryIT {

    private static final String BOOK_ID = "cccccccc-cccc-cccc-cccc-ccccccccccc1";

    @Autowired
    private LibraryService service;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @AfterEach
    void deleteRows() {
        loanRepository.deleteAll();
        bookRepository.deleteAll();
    }

    @Test
    void givenOneCopy_whenTwoUsersBorrowConcurrently_thenOnlyOneSucceeds() throws Exception {
        bookRepository.save(Book.builder()
                .id(BOOK_ID)
                .title("Race")
                .author("Author")
                .category("Tech")
                .totalCopies(1)
                .build());

        List<Object> results = Collections.synchronizedList(new ArrayList<>());
        Thread userA = new Thread(() -> results.add(tryBorrow("user-a")));
        Thread userB = new Thread(() -> results.add(tryBorrow("user-b")));
        userA.start();
        userB.start();
        userA.join();
        userB.join();

        assertThat(results).filteredOn(BorrowResult.class::isInstance).hasSize(1);
        assertThat(results).filteredOn(ELibraryException.class::isInstance)
                .singleElement()
                .extracting(result -> ((ELibraryException) result).getErrorCode())
                .isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
        assertThat(loanRepository.count()).isEqualTo(1);
    }

    private Object tryBorrow(String userId) {
        try {
            return service.borrowBook(userId, BOOK_ID);
        } catch (ELibraryException ex) {
            return ex;
        }
    }
}
