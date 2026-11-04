package com.elibrary.controller;

import com.elibrary.exception.GlobalExceptionHandler;
import com.elibrary.service.LibraryService;
import com.elibrary.service.dto.LoanWithTitle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LoansController.class)
@Import(GlobalExceptionHandler.class)
class LoansControllerTest {

    private static final UUID BOOK_ID = UUID.fromString("11111111-1111-1111-1111-111111111101");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LibraryService libraryService;


    @Test
    void givenMissingUserId_whenListLoans_thenReturn401() throws Exception {
        mockMvc.perform(get("/loans"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void givenUserHasLoans_whenListLoans_thenReturn200() throws Exception {
        Instant now = Instant.parse("2026-11-03T10:00:00Z");
        when(libraryService.listActiveLoans(anyString()))
                .thenReturn(List.of(new LoanWithTitle(
                        BOOK_ID.toString(),
                        "Clean Code",
                        now,
                        now.plusSeconds(86400L * 14)
                )));

        mockMvc.perform(get("/loans").header("X-User-Id", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookId").value(BOOK_ID.toString()))
                .andExpect(jsonPath("$[0].dueDate").exists());
    }
}
