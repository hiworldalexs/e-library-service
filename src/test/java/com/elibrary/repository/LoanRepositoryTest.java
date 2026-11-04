package com.elibrary.repository;

import com.elibrary.entity.Loan;
import com.elibrary.service.dto.BookLoanCount;
import com.elibrary.service.dto.LoanWithTitle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static com.elibrary.support.Fixtures.book;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class LoanRepositoryTest {

    private static final String CLEAN_CODE = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1";
    private static final String DDD = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2";
    private static final String PRIDE = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3";
    private static final Instant T1 = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-01-02T10:00:00Z");
    private static final Instant T3 = Instant.parse("2026-01-03T10:00:00Z");
    private static final Instant DUE = Instant.parse("2026-01-15T10:00:00Z");

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @BeforeEach
    void seed() {
        bookRepository.save(book().id(CLEAN_CODE).build());
        bookRepository.save(book().id(DDD).title("Domain-Driven Design").build());
        bookRepository.save(book().id(PRIDE).title("Pride and Prejudice").build());

        loanRepository.save(Loan.create("user-a", CLEAN_CODE, T1, DUE));
        loanRepository.save(Loan.create("user-a", DDD, T2, DUE));
        Loan returned = Loan.create("user-a", PRIDE, T3, DUE);
        returned.setReturnedAt(T3.plusSeconds(3600));
        loanRepository.save(returned);
        loanRepository.save(Loan.create("user-b", CLEAN_CODE, T2, DUE));
    }

    @Test
    void givenMixedLoans_whenFindActiveWithTitle_thenJoinBookAndSkipReturned() {
        List<LoanWithTitle> loans = loanRepository.findActiveLoansWithTitleByUserId("user-a");

        assertThat(loans).extracting(
                        LoanWithTitle::bookId,
                        LoanWithTitle::bookTitle,
                        LoanWithTitle::borrowedAt)
                .containsExactly(
                        tuple(CLEAN_CODE, "Clean Code", T1),
                        tuple(DDD, "Domain-Driven Design", T2)
                );
    }

    @Test
    void givenMixedLoans_whenCountActiveByBookIds_thenGroupActiveOnly() {
        List<BookLoanCount> counts = loanRepository.countActiveByBookIds(
                List.of(CLEAN_CODE, DDD, PRIDE));

        assertThat(counts).extracting(BookLoanCount::bookId, BookLoanCount::count)
                .containsExactlyInAnyOrder(
                        tuple(CLEAN_CODE, 2L),
                        tuple(DDD, 1L)
                );
    }
}
