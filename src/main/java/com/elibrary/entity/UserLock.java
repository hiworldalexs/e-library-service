package com.elibrary.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_locks")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserLock {

    @Id
    @Column(length = 128, nullable = false, updatable = false)
    private String userId;
}
