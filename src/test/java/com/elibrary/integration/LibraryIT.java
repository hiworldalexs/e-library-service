package com.elibrary.integration;

import com.elibrary.config.ELibraryProperties;
import com.elibrary.entity.Book;
import com.elibrary.exception.ELibraryException;
import com.elibrary.exception.ErrorCode;
import com.elibrary.repository.BookRepository;
import com.elibrary.repository.LoanRepository;
import com.elibrary.repository.UserLockRepository;
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
    private static final String OTHER_BOOK_ID = "cccccccc-cccc-cccc-cccc-ccccccccccc2";

    @Autowired
    private LibraryService service;

    @Autowired
    private ELibraryProperties properties;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserLockRepository userLockRepository;

    @AfterEach
    void deleteRows() {
        loanRepository.deleteAll();
        userLockRepository.deleteAll();
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

    @Test
    void givenOneBorrowSlot_whenSameUserBorrowsTwoBooksConcurrently_thenOnlyOneSucceeds() throws Exception {
        saveBook(BOOK_ID);
        saveBook(OTHER_BOOK_ID);
        int configuredLimit = properties.getMaxConcurrentBorrows();
        properties.setMaxConcurrentBorrows(1);

        try {
            List<Object> results = Collections.synchronizedList(new ArrayList<>());
            Thread first = new Thread(() -> results.add(tryBorrow("user-a", BOOK_ID)));
            Thread second = new Thread(() -> results.add(tryBorrow("user-a", OTHER_BOOK_ID)));
            first.start();
            second.start();
            first.join();
            second.join();

            assertThat(results).filteredOn(BorrowResult.class::isInstance).hasSize(1);
            assertThat(results).filteredOn(ELibraryException.class::isInstance)
                    .singleElement()
                    .extracting(result -> ((ELibraryException) result).getErrorCode())
                    .isEqualTo(ErrorCode.BORROW_LIMIT_EXCEEDED);
            assertThat(loanRepository.count()).isEqualTo(1);
        } finally {
            properties.setMaxConcurrentBorrows(configuredLimit);
        }
    }

    private void saveBook(String bookId) {
        bookRepository.save(Book.builder()
                .id(bookId)
                .title("Race")
                .author("Author")
                .category("Tech")
                .totalCopies(1)
                .build());
    }

    private Object tryBorrow(String userId) {
        return tryBorrow(userId, BOOK_ID);
    }

    private Object tryBorrow(String userId, String bookId) {
        try {
            return service.borrowBook(userId, bookId);
        } catch (ELibraryException ex) {
            return ex;
        }
    }
}
