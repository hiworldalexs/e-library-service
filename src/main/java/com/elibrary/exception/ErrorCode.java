package com.elibrary.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND),
    BOOK_NOT_AVAILABLE(HttpStatus.CONFLICT),
    BORROW_LIMIT_EXCEEDED(HttpStatus.CONFLICT),
    USER_HAS_OVERDUE_LOAN(HttpStatus.CONFLICT),
    BOOK_ALREADY_BORROWED(HttpStatus.CONFLICT),
    BOOK_NOT_BORROWED_BY_USER(HttpStatus.CONFLICT),
    OPTIMISTIC_LOCK_ERROR(HttpStatus.CONFLICT),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
