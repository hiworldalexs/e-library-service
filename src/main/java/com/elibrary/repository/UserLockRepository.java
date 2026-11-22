package com.elibrary.repository;

import com.elibrary.entity.UserLock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserLockRepository extends JpaRepository<UserLock, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserLock u WHERE u.userId = :userId")
    Optional<UserLock> findByIdForWrite(@Param("userId") String userId);
}
