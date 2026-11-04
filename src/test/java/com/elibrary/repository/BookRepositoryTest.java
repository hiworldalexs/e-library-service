package com.elibrary.repository;

import com.elibrary.entity.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static com.elibrary.support.Fixtures.book;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class BookRepositoryTest {

    private static final String CLEAN_CODE = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1";
    private static final String DDD = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2";
    private static final String PRIDE = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3";

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void seedBooks() {
        bookRepository.save(book().id(CLEAN_CODE).author("Robert Martin").build());
        bookRepository.save(book().id(DDD).title("Domain-Driven Design").author("Henry Evans").build());
        bookRepository.save(book().id(PRIDE).title("Pride and Prejudice").author("Jane Austen").category("Fiction").build());
    }

    @Test
    void givenNoFilters_whenFindByFilters_thenReturnAllBooksPaged() {
        Page<Book> page = bookRepository.findByFilters(
                null, null, null, PageRequest.of(0, 2, Sort.by("title")));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(Book::getTitle)
                .containsExactly("Clean Code", "Domain-Driven Design");
    }

    @Test
    void givenTitleFragment_whenFindByFilters_thenMatchCaseInsensitiveLike() {
        Page<Book> page = bookRepository.findByFilters(
                "clean", null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Book::getId).containsExactly(CLEAN_CODE);
    }

    @Test
    void givenAuthorAndCategory_whenFindByFilters_thenLikeAuthorAndExactCategory() {
        Page<Book> techMartin = bookRepository.findByFilters(
                null, "martin", "tech", PageRequest.of(0, 10));
        Page<Book> fictionMartin = bookRepository.findByFilters(
                null, "martin", "Fiction", PageRequest.of(0, 10));

        assertThat(techMartin.getContent()).extracting(Book::getId).containsExactly(CLEAN_CODE);
        assertThat(fictionMartin.getContent()).isEmpty();
    }
}
