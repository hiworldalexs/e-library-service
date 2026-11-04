package com.elibrary.exception;

import com.elibrary.api.model.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(basePackages = "com.elibrary")
public class GlobalExceptionHandler {

    private static final String DEFAULT_MESSAGE = "Not specified";

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex) {
        return respond(ErrorCode.OPTIMISTIC_LOCK_ERROR,
                "The loan was updated by another request. Please retry.", ex);
    }

    @ExceptionHandler(ELibraryException.class)
    public ResponseEntity<ErrorResponse> handleELibraryException(ELibraryException ex) {
        return respond(ex.getErrorCode(), ex.getMessage(), ex);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        ErrorCode code = "X-User-Id".equalsIgnoreCase(ex.getHeaderName())
                ? ErrorCode.UNAUTHENTICATED
                : ErrorCode.INVALID_REQUEST;
        return respond(code, DEFAULT_MESSAGE, ex);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception ex) {
        return respond(ErrorCode.INVALID_REQUEST, clientMessage(ex), ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return respond(ErrorCode.INTERNAL_SERVER_ERROR, DEFAULT_MESSAGE, ex);
    }

    private static String clientMessage(Exception ex) {
        return switch (ex) {
            case MethodArgumentNotValidException e -> e.getBindingResult().getFieldErrors().stream()
                    .map(err -> err.getField() + ": " + err.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            case ConstraintViolationException e -> e.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            case HandlerMethodValidationException e -> e.getAllErrors().stream()
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining("; "));
            case MethodArgumentTypeMismatchException e when e.getName() != null ->
                    e.getName() + ": invalid value";
            default -> ex.getMessage();
        };
    }

    private static ResponseEntity<ErrorResponse> respond(ErrorCode code, String message, Exception ex) {
        ErrorResponse body = error(code, message);
        if (code.getHttpStatus().is5xxServerError()) {
            log.error("{}: {}", code, body.message(), ex);
        } else {
            log.warn("{}: {}", code, body.message());
        }
        return ResponseEntity.status(code.getHttpStatus()).body(body);
    }

    private static ErrorResponse error(ErrorCode code, String message) {
        String resolved = (message == null || message.isBlank()) ? DEFAULT_MESSAGE : message;
        return new ErrorResponse(code.name(), resolved);
    }
}
