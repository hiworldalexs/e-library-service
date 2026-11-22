package com.elibrary.service;

import com.elibrary.config.ELibraryProperties;
import com.elibrary.entity.Book;
import com.elibrary.entity.Loan;
import com.elibrary.exception.ELibraryException;
import com.elibrary.exception.ErrorCode;
import com.elibrary.repository.BookRepository;
import com.elibrary.repository.LoanRepository;
import com.elibrary.service.dto.BookLoanCount;
import com.elibrary.service.dto.BookWithAvailability;
import com.elibrary.service.dto.BorrowResult;
import com.elibrary.service.dto.LoanWithTitle;
import com.elibrary.service.dto.ReturnResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-11-03T10:00:00Z");
    private static final String USER_A = "user-a";
    private static final String USER_B = "user-b";
    private static final String SINGLE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1";
    private static final String MULTI_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2";

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private UserLockService userLockService;

    private ELibraryProperties properties;
    private LibraryService service;

    private Book singleCopyBook;
    private Book multiCopyBook;

    @BeforeEach
    void setUp() {
        properties = new ELibraryProperties();
        properties.setMaxConcurrentBorrows(3);
        properties.setLoanPeriodDays(14);

        Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        service = new LibraryService(bookRepository, loanRepository, properties, clock, userLockService);

        singleCopyBook = Book.builder()
                .id(SINGLE_ID)
                .title("Single Copy")
                .author("Alice Author")
                .category("Tech")
                .totalCopies(1)
                .build();
        multiCopyBook = Book.builder()
                .id(MULTI_ID)
                .title("Multi Copy")
                .author("Bob Author")
                .category("Tech")
                .totalCopies(2)
                .build();
    }

    @Test
    void givenBlankFilters_whenBrowseBooks_thenPassNullsAndMapAvailability() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "title"));
        when(bookRepository.findByFilters(isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(singleCopyBook, multiCopyBook), pageable, 2));
        when(loanRepository.countActiveByBookIds(List.of(SINGLE_ID, MULTI_ID)))
                .thenReturn(List.of(new BookLoanCount(SINGLE_ID, 1L)));

        Page<BookWithAvailability> page = service.browseBooks(pageable, "  ", "", null);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).availableCopies()).isZero();
        assertThat(page.getContent().get(1).availableCopies()).isEqualTo(2);
        verify(bookRepository).findByFilters(isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    void givenTitleAuthorAndCategory_whenBrowseBooks_thenForwardTrimmedFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        when(bookRepository.findByFilters(eq("copy"), eq("bob"), eq("Tech"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(multiCopyBook), pageable, 1));
        when(loanRepository.countActiveByBookIds(List.of(MULTI_ID))).thenReturn(List.of());

        Page<BookWithAvailability> page = service.browseBooks(pageable, " copy ", " bob ", "Tech");

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().book().getTitle()).isEqualTo("Multi Copy");
        assertThat(page.getContent().getFirst().availableCopies()).isEqualTo(2);
        verify(bookRepository).findByFilters(eq("copy"), eq("bob"), eq("Tech"), eq(pageable));
    }

    @Test
    void givenFirstPageWithKnownTotal_whenBrowseBooks_thenReturnContentAndTotals() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "title"));
        when(bookRepository.findByFilters(isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(singleCopyBook, multiCopyBook), pageable, 25));
        when(loanRepository.countActiveByBookIds(List.of(SINGLE_ID, MULTI_ID))).thenReturn(List.of());

        Page<BookWithAvailability> page = service.browseBooks(pageable, null, null, null);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).book().getId()).isEqualTo(SINGLE_ID);
        assertThat(page.getContent().get(1).book().getId()).isEqualTo(MULTI_ID);
        assertThat(page.getTotalElements()).isEqualTo(25);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getNumber()).isZero();
        assertThat(page.getSize()).isEqualTo(10);
        verify(bookRepository).findByFilters(isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    void givenPageBeyondResults_whenBrowseBooks_thenReturnEmptyContentWithCorrectTotals() {
        Pageable pageable = PageRequest.of(5, 10);
        when(bookRepository.findByFilters(isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 12));

        Page<BookWithAvailability> page = service.browseBooks(pageable, null, null, null);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(12);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getNumber()).isEqualTo(5);
        verify(loanRepository, never()).countActiveByBookIds(anyCollection());
        verify(bookRepository).findByFilters(isNull(), isNull(), isNull(), eq(pageable));
    }

    @Test
    void givenCustomPageSize_whenBrowseBooks_thenForwardPageableAndReflectSize() {
        Pageable pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "author"));
        when(bookRepository.findByFilters(isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(multiCopyBook), pageable, 11));
        when(loanRepository.countActiveByBookIds(List.of(MULTI_ID))).thenReturn(List.of());

        Page<BookWithAvailability> page = service.browseBooks(pageable, null, null, null);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().book().getId()).isEqualTo(MULTI_ID);
        assertThat(page.getSize()).isEqualTo(5);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getTotalElements()).isEqualTo(11);
        assertThat(page.getTotalPages()).isEqualTo(3);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(bookRepository).findByFilters(isNull(), isNull(), isNull(), pageableCaptor.capture());
        Pageable forwarded = pageableCaptor.getValue();
        assertThat(forwarded.getPageNumber()).isEqualTo(1);
        assertThat(forwarded.getPageSize()).isEqualTo(5);
        assertThat(forwarded.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "author"));
    }

    @Test
    void givenExistingBook_whenGetBook_thenReturnAvailableCopies() {
        when(bookRepository.findById(MULTI_ID)).thenReturn(Optional.of(multiCopyBook));
        when(loanRepository.countByBookIdAndReturnedAtIsNull(MULTI_ID)).thenReturn(0L);

        BookWithAvailability response = service.getBook(MULTI_ID);

        assertThat(response.availableCopies()).isEqualTo(2);
        assertThat(response.book().getTotalCopies()).isEqualTo(2);
        assertThat(response.book().getAuthor()).isEqualTo("Bob Author");
    }

    @Test
    void givenActiveLoans_whenGetBook_thenSubtractFromTotalCopies() {
        when(bookRepository.findById(MULTI_ID)).thenReturn(Optional.of(multiCopyBook));
        when(loanRepository.countByBookIdAndReturnedAtIsNull(MULTI_ID)).thenReturn(1L);

        BookWithAvailability response = service.getBook(MULTI_ID);

        assertThat(response.availableCopies()).isEqualTo(1);
    }

    @Test
    void givenUnknownBookId_whenGetBook_thenThrowBookNotFound() {
        when(bookRepository.findById("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")).thenReturn(Optional.empty());

        assertThatExceptionOfType(ELibraryException.class)
                .isThrownBy(() -> service.getBook("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
                .extracting(ELibraryException::getErrorCode)
                .isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    void givenAvailableBook_whenBorrowBook_thenSetDueDateFromConfig() {
        properties.setLoanPeriodDays(7);
        stubBorrowPreconditions(USER_A, singleCopyBook, 0, false, 0);

        BorrowResult borrowed = service.borrowBook(USER_A, SINGLE_ID);

        assertThat(borrowed.book().getId()).isEqualTo(SINGLE_ID);
        assertThat(borrowed.loan().getBorrowedAt()).isEqualTo(FIXED_NOW);
        assertThat(borrowed.loan().getDueDate()).isEqualTo(FIXED_NOW.plus(7, ChronoUnit.DAYS));
        assertThat(borrowed.loan().getUserId()).isEqualTo(USER_A);
        assertThat(borrowed.loan().getBookId()).isEqualTo(SINGLE_ID);
        assertThat(borrowed.loan().getReturnedAt()).isNull();

        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(loanCaptor.capture());
        assertThat(loanCaptor.getValue().getDueDate()).isEqualTo(FIXED_NOW.plus(7, ChronoUnit.DAYS));
    }

    @Test
    void givenNoCopiesLeft_whenBorrowBook_thenThrowBookNotAvailable() {
        stubBorrowPreconditions(USER_B, singleCopyBook, 0, false, 1);

        assertThatExceptionOfType(ELibraryException.class)
                .isThrownBy(() -> service.borrowBook(USER_B, SINGLE_ID))
                .extracting(ELibraryException::getErrorCode)
                .isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE);
        verify(loanRepository, never()).save(any());
    }

    @Test
    void givenActiveLoanForSameBook_whenBorrowBook_thenThrowBookAlreadyBorrowed() {
        when(loanRepository.existsByUserIdAndReturnedAtIsNullAndDueDateBefore(USER_A, FIXED_NOW))
                .thenReturn(false);
        when(loanRepository.countByUserIdAndReturnedAtIsNull(USER_A)).thenReturn(1L);
        when(bookRepository.findByIdForWrite(MULTI_ID)).thenReturn(Optional.of(multiCopyBook));
        when(loanRepository.findByUserIdAndBookIdAndReturnedAtIsNull(USER_A, MULTI_ID))
                .thenReturn(Optional.of(Loan.create(USER_A, MULTI_ID, FIXED_NOW, FIXED_NOW.plus(14, ChronoUnit.DAYS))));

        assertThatExceptionOfType(ELibraryException.class)
                .isThrownBy(() -> service.borrowBook(USER_A, MULTI_ID))
                .extracting(ELibraryException::getErrorCode)
                .isEqualTo(ErrorCode.BOOK_ALREADY_BORROWED);
        verify(loanRepository, never()).save(any());
    }

    @Test
    void givenOverdueLoan_whenBorrowBook_thenThrowUserHasOverdueLoan() {
        when(loanRepository.existsByUserIdAndReturnedAtIsNullAndDueDateBefore(USER_A, FIXED_NOW))
                .thenReturn(true);

        assertThatExceptionOfType(ELibraryException.class)
                .isThrownBy(() -> service.borrowBook(USER_A, SINGLE_ID))
                .extracting(ELibraryException::getErrorCode)
                .isEqualTo(ErrorCode.USER_HAS_OVERDUE_LOAN);
        verify(bookRepository, never()).findByIdForWrite(any());
        verify(loanRepository, never()).save(any());
    }

    @Test
    void givenBorrowLimitReached_whenBorrowBook_thenThrowBorrowLimitExceeded() {
        properties.setMaxConcurrentBorrows(1);
        when(loanRepository.existsByUserIdAndReturnedAtIsNullAndDueDateBefore(USER_A, FIXED_NOW))
                .thenReturn(false);
        when(loanRepository.countByUserIdAndReturnedAtIsNull(USER_A)).thenReturn(1L);

        assertThatExceptionOfType(ELibraryException.class)
                .isThrownBy(() -> service.borrowBook(USER_A, SINGLE_ID))
                .extracting(ELibraryException::getErrorCode)
                .isEqualTo(ErrorCode.BORROW_LIMIT_EXCEEDED);
        verify(bookRepository, never()).findByIdForWrite(any());
        verify(loanRepository, never()).save(any());
    }

    @Test
    void givenUnknownBook_whenBorrowBook_thenThrowBookNotFound() {
        when(loanRepository.existsByUserIdAndReturnedAtIsNullAndDueDateBefore(USER_A, FIXED_NOW))
                .thenReturn(false);
        when(loanRepository.countByUserIdAndReturnedAtIsNull(USER_A)).thenReturn(0L);
        when(bookRepository.findByIdForWrite(SINGLE_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ELibraryException.class)
                .isThrownBy(() -> service.borrowBook(USER_A, SINGLE_ID))
                .extracting(ELibraryException::getErrorCode)
                .isEqualTo(ErrorCode.BOOK_NOT_FOUND);
    }

    @Test
    void givenActiveLoan_whenReturnBook_thenClearLoan() {
        Loan active = Loan.create(USER_A, MULTI_ID, FIXED_NOW.minus(1, ChronoUnit.DAYS),
                FIXED_NOW.plus(13, ChronoUnit.DAYS));
        when(bookRepository.findById(MULTI_ID)).thenReturn(Optional.of(multiCopyBook));
        when(loanRepository.findByUserIdAndBookIdAndReturnedAtIsNull(USER_A, MULTI_ID))
                .thenReturn(Optional.of(active));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReturnResult returned = service.returnBook(USER_A, MULTI_ID);

        assertThat(returned.loan().getReturnedAt()).isEqualTo(FIXED_NOW);
        assertThat(returned.book().getId()).isEqualTo(MULTI_ID);
        verify(loanRepository).save(active);
    }

    @Test
    void givenNoActiveLoan_whenReturnBook_thenThrowBookNotBorrowedByUser() {
        when(bookRepository.findById(MULTI_ID)).thenReturn(Optional.of(multiCopyBook));
        when(loanRepository.findByUserIdAndBookIdAndReturnedAtIsNull(USER_A, MULTI_ID))
                .thenReturn(Optional.empty());

        assertThatExceptionOfType(ELibraryException.class)
                .isThrownBy(() -> service.returnBook(USER_A, MULTI_ID))
                .extracting(ELibraryException::getErrorCode)
                .isEqualTo(ErrorCode.BOOK_NOT_BORROWED_BY_USER);
        verify(loanRepository, never()).save(any());
    }

    @Test
    void givenActiveLoans_whenListActiveLoans_thenDelegateToRepository() {
        List<LoanWithTitle> expected = List.of(
                new LoanWithTitle(MULTI_ID, "Multi Copy", FIXED_NOW, FIXED_NOW.plus(14, ChronoUnit.DAYS)));
        when(loanRepository.findActiveLoansWithTitleByUserId(USER_A)).thenReturn(expected);

        List<LoanWithTitle> loans = service.listActiveLoans(USER_A);

        assertThat(loans).isEqualTo(expected);
        verify(loanRepository).findActiveLoansWithTitleByUserId(USER_A);
    }

    private void stubBorrowPreconditions(
            String userId,
            Book book,
            long activeForUser,
            boolean alreadyBorrowed,
            long activeForBook) {
        when(loanRepository.existsByUserIdAndReturnedAtIsNullAndDueDateBefore(userId, FIXED_NOW))
                .thenReturn(false);
        when(loanRepository.countByUserIdAndReturnedAtIsNull(userId)).thenReturn(activeForUser);
        when(bookRepository.findByIdForWrite(book.getId())).thenReturn(Optional.of(book));
        when(loanRepository.findByUserIdAndBookIdAndReturnedAtIsNull(userId, book.getId()))
                .thenReturn(alreadyBorrowed
                        ? Optional.of(Loan.create(userId, book.getId(), FIXED_NOW, FIXED_NOW.plus(14, ChronoUnit.DAYS)))
                        : Optional.empty());
        if (!alreadyBorrowed) {
            when(loanRepository.countByBookIdAndReturnedAtIsNull(book.getId())).thenReturn(activeForBook);
        }
        if (!alreadyBorrowed && activeForBook < book.getTotalCopies()) {
            when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));
        }
    }
}
