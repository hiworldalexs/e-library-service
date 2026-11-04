package com.elibrary.mapper;

import com.elibrary.api.model.BookResponse;
import com.elibrary.api.model.BorrowResponse;
import com.elibrary.api.model.LoanResponse;
import com.elibrary.api.model.PagedBooksResponse;
import com.elibrary.api.model.ReturnResponse;
import com.elibrary.entity.Book;
import com.elibrary.entity.Loan;
import com.elibrary.service.dto.BookWithAvailability;
import com.elibrary.service.dto.BorrowResult;
import com.elibrary.service.dto.LoanWithTitle;
import com.elibrary.service.dto.ReturnResult;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public final class LibraryMapper {

    private LibraryMapper() {
    }

    public static BookResponse toBookResponse(BookWithAvailability book) {
        return toBookResponse(book.book(), book.availableCopies());
    }

    public static BookResponse toBookResponse(Book book, int availableCopies) {
        return new BookResponse(
                UUID.fromString(book.getId()),
                book.getTitle(),
                book.getAuthor(),
                book.getCategory(),
                book.getTotalCopies(),
                availableCopies
        );
    }

    public static PagedBooksResponse toPagedBooksResponse(Page<BookWithAvailability> page) {
        List<BookResponse> content = page.getContent().stream()
                .map(LibraryMapper::toBookResponse)
                .toList();
        return new PagedBooksResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    public static BorrowResponse toBorrowResponse(BorrowResult result) {
        return toBorrowResponse(result.book(), result.loan());
    }

    public static BorrowResponse toBorrowResponse(Book book, Loan loan) {
        return new BorrowResponse(
                UUID.fromString(book.getId()),
                book.getTitle(),
                loan.getBorrowedAt(),
                loan.getDueDate()
        );
    }

    public static ReturnResponse toReturnResponse(ReturnResult result) {
        return toReturnResponse(result.book(), result.loan());
    }

    public static ReturnResponse toReturnResponse(Book book, Loan loan) {
        return new ReturnResponse(
                UUID.fromString(book.getId()),
                book.getTitle(),
                loan.getReturnedAt()
        );
    }

    public static LoanResponse toLoanResponse(LoanWithTitle loan) {
        return new LoanResponse(
                UUID.fromString(loan.bookId()),
                loan.bookTitle(),
                loan.borrowedAt(),
                loan.dueDate()
        );
    }

    public static List<LoanResponse> toLoanResponses(List<LoanWithTitle> loans) {
        return loans.stream().map(LibraryMapper::toLoanResponse).toList();
    }
}
