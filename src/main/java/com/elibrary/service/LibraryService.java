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
import com.elibrary.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final ELibraryProperties properties;
    private final Clock clock;
    private final UserLockService userLockService;

    @Transactional(readOnly = true)
    public Page<BookWithAvailability> browseBooks(Pageable pageable, String title, String author, String category) {
        Page<Book> result = bookRepository.findByFilters(
                StringUtils.blankToNull(title),
                StringUtils.blankToNull(author),
                StringUtils.blankToNull(category),
                pageable
        );

        Map<String, Long> activeByBookId = activeLoanCounts(result.getContent());
        List<BookWithAvailability> content = result.getContent().stream()
                .map(book -> toBookWithAvailability(book, activeByBookId))
                .toList();

        return new PageImpl<>(content, pageable, result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public BookWithAvailability getBook(String bookId) {
        Book book = requireBook(bookId);
        long active = loanRepository.countByBookIdAndReturnedAtIsNull(book.getId());
        return new BookWithAvailability(book, availableCopies(book, active));
    }

    @Transactional
    public BorrowResult borrowBook(String userId, String bookId) {
        try {
            userLockService.ensureExists(userId);
        } catch (DataIntegrityViolationException ex) {
            log.debug("User lock already exists for userId={}", userId);
        }
        userLockService.lock(userId);

        Instant borrowedAt = Instant.now(clock);
        if (loanRepository.existsByUserIdAndReturnedAtIsNullAndDueDateBefore(userId, borrowedAt)) {
            throw new ELibraryException(ErrorCode.USER_HAS_OVERDUE_LOAN);
        }

        long activeForUser = loanRepository.countByUserIdAndReturnedAtIsNull(userId);
        if (activeForUser >= properties.getMaxConcurrentBorrows()) {
            throw new ELibraryException(ErrorCode.BORROW_LIMIT_EXCEEDED);
        }

        Book book = requireBookForWrite(bookId);

        if (loanRepository.findByUserIdAndBookIdAndReturnedAtIsNull(userId, bookId).isPresent()) {
            throw new ELibraryException(ErrorCode.BOOK_ALREADY_BORROWED);
        }

        long activeForBook = loanRepository.countByBookIdAndReturnedAtIsNull(bookId);
        if (activeForBook >= book.getTotalCopies()) {
            throw new ELibraryException(ErrorCode.BOOK_NOT_AVAILABLE);
        }

        Instant dueDate = borrowedAt.plus(properties.getLoanPeriodDays(), ChronoUnit.DAYS);
        Loan loan = Loan.create(userId, bookId, borrowedAt, dueDate);
        loanRepository.save(loan);
        log.info("borrowed bookId={} userId={} loanId={} dueDate={}", bookId, userId, loan.getId(), dueDate);
        return new BorrowResult(book, loan);
    }

    @Transactional
    public ReturnResult returnBook(String userId, String bookId) {
        Book book = requireBook(bookId);
        Loan loan = loanRepository.findByUserIdAndBookIdAndReturnedAtIsNull(userId, bookId)
                .orElseThrow(() -> new ELibraryException(ErrorCode.BOOK_NOT_BORROWED_BY_USER));

        Instant returnedAt = Instant.now(clock);
        loan.setReturnedAt(returnedAt);
        loanRepository.save(loan);
        log.info("returned bookId={} userId={} loanId={}", bookId, userId, loan.getId());
        return new ReturnResult(book, loan);
    }

    @Transactional(readOnly = true)
    public List<LoanWithTitle> listActiveLoans(String userId) {
        return loanRepository.findActiveLoansWithTitleByUserId(userId);
    }

    private Book requireBookForWrite(String bookId) {
        return bookRepository.findByIdForWrite(bookId)
                .orElseThrow(() -> new ELibraryException(ErrorCode.BOOK_NOT_FOUND));
    }

    private Book requireBook(String bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new ELibraryException(ErrorCode.BOOK_NOT_FOUND));
    }

    private Map<String, Long> activeLoanCounts(List<Book> books) {
        if (books.isEmpty()) {
            return Map.of();
        }
        List<String> bookIds = books.stream().map(Book::getId).toList();
        Map<String, Long> counts = new HashMap<>();
        for (BookLoanCount row : loanRepository.countActiveByBookIds(bookIds)) {
            counts.put(row.bookId(), row.count());
        }
        return counts;
    }

    private BookWithAvailability toBookWithAvailability(Book book, Map<String, Long> activeByBookId) {
        long active = activeByBookId.getOrDefault(book.getId(), 0L);
        return new BookWithAvailability(book, availableCopies(book, active));
    }

    private int availableCopies(Book book, long activeLoans) {
        return (int) Math.max(0, book.getTotalCopies() - activeLoans);
    }
}
