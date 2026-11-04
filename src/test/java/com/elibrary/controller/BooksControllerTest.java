package com.elibrary.controller;

import com.elibrary.entity.Book;
import com.elibrary.entity.Loan;
import com.elibrary.exception.GlobalExceptionHandler;
import com.elibrary.service.LibraryService;
import com.elibrary.service.dto.BookWithAvailability;
import com.elibrary.service.dto.BorrowResult;
import com.elibrary.service.dto.ReturnResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BooksController.class)
@Import(GlobalExceptionHandler.class)
class BooksControllerTest {

    private static final UUID BOOK_ID = UUID.fromString("11111111-1111-1111-1111-111111111101");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LibraryService libraryService;


    @Test
    void givenInvalidSize_whenBrowseBooks_thenReturn400() throws Exception {
        mockMvc.perform(get("/books").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void givenInvalidSort_whenBrowseBooks_thenReturn400() throws Exception {
        mockMvc.perform(get("/books").param("sort", "isbn,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid sort"));
    }

    @Test
    void givenAuthorDescSort_whenBrowseBooks_thenReturn200() throws Exception {
        Page<BookWithAvailability> empty = new PageImpl<>(List.of());
        when(libraryService.browseBooks(any(Pageable.class), isNull(), isNull(), isNull()))
                .thenReturn(empty);

        mockMvc.perform(get("/books").param("sort", "author,desc"))
                .andExpect(status().isOk());

        verify(libraryService).browseBooks(any(Pageable.class), isNull(), isNull(), isNull());
    }


    @Test
    void givenMissingUserId_whenBorrowBook_thenReturn401() throws Exception {
        mockMvc.perform(post("/books/{bookId}/borrow", BOOK_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("Not specified"));
    }

    @Test
    void givenEmptyUserId_whenBorrowBook_thenReturn400() throws Exception {
        mockMvc.perform(post("/books/{bookId}/borrow", BOOK_ID).header("X-User-Id", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void givenAvailableCopies_whenBorrowBook_thenReturn200() throws Exception {
        Instant now = Instant.parse("2026-11-03T10:00:00Z");
        Book book = Book.builder()
                .id(BOOK_ID.toString())
                .title("Clean Code")
                .author("Robert C. Martin")
                .category("Tech")
                .totalCopies(2)
                .build();
        Loan loan = Loan.create("alice", BOOK_ID.toString(), now, now.plusSeconds(86400L * 14));
        when(libraryService.borrowBook(eq("alice"), eq(BOOK_ID.toString())))
                .thenReturn(new BorrowResult(book, loan));

        mockMvc.perform(post("/books/{bookId}/borrow", BOOK_ID).header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(BOOK_ID.toString()))
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.borrowedAt").exists())
                .andExpect(jsonPath("$.dueDate").exists());
    }


    @Test
    void givenMissingUserId_whenReturnBook_thenReturn401() throws Exception {
        mockMvc.perform(post("/books/{bookId}/return", BOOK_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void givenUserHoldsBook_whenReturnBook_thenReturn200() throws Exception {
        Instant now = Instant.parse("2026-11-03T10:00:00Z");
        Book book = Book.builder()
                .id(BOOK_ID.toString())
                .title("Clean Code")
                .author("Robert C. Martin")
                .category("Tech")
                .totalCopies(2)
                .build();
        Loan loan = Loan.create("alice", BOOK_ID.toString(), now.minusSeconds(3600), now.plusSeconds(86400L * 13));
        loan.setReturnedAt(now);
        when(libraryService.returnBook(eq("alice"), eq(BOOK_ID.toString())))
                .thenReturn(new ReturnResult(book, loan));

        mockMvc.perform(post("/books/{bookId}/return", BOOK_ID).header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(BOOK_ID.toString()))
                .andExpect(jsonPath("$.returnedAt").exists());
    }
}
