package com.elibrary.repository;

import com.elibrary.entity.Loan;
import com.elibrary.service.dto.BookLoanCount;
import com.elibrary.service.dto.LoanWithTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, String> {

    @Query("""
            SELECT new com.elibrary.service.dto.LoanWithTitle(
                l.bookId,
                b.title,
                l.borrowedAt,
                l.dueDate
            )
            FROM Loan l JOIN Book b ON b.id = l.bookId
            WHERE l.userId = :userId AND l.returnedAt IS NULL
            ORDER BY l.borrowedAt ASC
            """)
    List<LoanWithTitle> findActiveLoansWithTitleByUserId(@Param("userId") String userId);

    long countByUserIdAndReturnedAtIsNull(String userId);

    boolean existsByUserIdAndReturnedAtIsNullAndDueDateBefore(String userId, Instant dueDate);

    long countByBookIdAndReturnedAtIsNull(String bookId);

    @Query("""
            SELECT new com.elibrary.service.dto.BookLoanCount(l.bookId, COUNT(l))
            FROM Loan l
            WHERE l.bookId IN :bookIds AND l.returnedAt IS NULL
            GROUP BY l.bookId
            """)
    List<BookLoanCount> countActiveByBookIds(@Param("bookIds") Collection<String> bookIds);

    Optional<Loan> findByUserIdAndBookIdAndReturnedAtIsNull(String userId, String bookId);
}
