package com.elibrary.service;

import com.elibrary.entity.UserLock;
import com.elibrary.repository.UserLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserLockService {

    private final UserLockRepository userLockRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureExists(String userId) {
        if (!userLockRepository.existsById(userId)) {
            userLockRepository.saveAndFlush(new UserLock(userId));
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(String userId) {
        userLockRepository.findByIdForWrite(userId)
                .orElseThrow(() -> new IllegalStateException("User lock row is missing for " + userId));
    }
}
