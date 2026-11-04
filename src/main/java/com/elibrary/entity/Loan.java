package com.elibrary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "loans",
        indexes = {
                @Index(name = "idx_loans_user_active", columnList = "userId"),
                @Index(name = "idx_loans_book_active", columnList = "bookId")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, length = 128)
    private String userId;

    @Column(nullable = false, length = 36)
    private String bookId;

    @Column(nullable = false)
    private Instant borrowedAt;

    @Column(nullable = false)
    private Instant dueDate;

    private Instant returnedAt;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public boolean isActive() {
        return returnedAt == null;
    }

    public static Loan create(String userId, String bookId, Instant borrowedAt, Instant dueDate) {
        return Loan.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .bookId(bookId)
                .borrowedAt(borrowedAt)
                .dueDate(dueDate)
                .returnedAt(null)
                .build();
    }
}
