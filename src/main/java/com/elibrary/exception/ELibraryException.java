package com.elibrary.exception;

public class ELibraryException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Not specified";

    private final ErrorCode errorCode;

    public ELibraryException(ErrorCode errorCode) {
        super(DEFAULT_MESSAGE);
        this.errorCode = errorCode;
    }

    public ELibraryException(ErrorCode errorCode, String message) {
        super(resolveMessage(message));
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    private static String resolveMessage(String message) {
        return (message == null || message.isBlank()) ? DEFAULT_MESSAGE : message;
    }
}
