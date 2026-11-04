package com.elibrary.controller;

import com.elibrary.api.BooksApi;
import com.elibrary.api.model.BookResponse;
import com.elibrary.api.model.BorrowResponse;
import com.elibrary.api.model.PagedBooksResponse;
import com.elibrary.api.model.ReturnResponse;
import com.elibrary.exception.ELibraryException;
import com.elibrary.exception.ErrorCode;
import com.elibrary.mapper.LibraryMapper;
import com.elibrary.service.LibraryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
public class BooksController implements BooksApi {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("title", "author", "category");

    private final LibraryService libraryService;

    public BooksController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @Override
    public ResponseEntity<PagedBooksResponse> browseBooks(
            Integer page,
            Integer size,
            String title,
            String author,
            String category,
            String sort
    ) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? 20 : size;
        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, toSort(sort));
        return ResponseEntity.ok(
                LibraryMapper.toPagedBooksResponse(
                        libraryService.browseBooks(pageable, title, author, category)
                )
        );
    }

    @Override
    public ResponseEntity<BookResponse> getBook(UUID bookId) {
        return ResponseEntity.ok(
                LibraryMapper.toBookResponse(libraryService.getBook(bookId.toString()))
        );
    }

    @Override
    public ResponseEntity<BorrowResponse> borrowBook(UUID bookId, String xUserId) {
        return ResponseEntity.ok(
                LibraryMapper.toBorrowResponse(libraryService.borrowBook(xUserId, bookId.toString()))
        );
    }

    @Override
    public ResponseEntity<ReturnResponse> returnBook(UUID bookId, String xUserId) {
        return ResponseEntity.ok(
                LibraryMapper.toReturnResponse(libraryService.returnBook(xUserId, bookId.toString()))
        );
    }

    private Sort toSort(String sortParam) {
        String resolved = (sortParam == null || sortParam.isBlank()) ? "title,asc" : sortParam.trim();
        String[] parts = resolved.split(",", 2);
        if (parts.length != 2) {
            throw new ELibraryException(ErrorCode.INVALID_REQUEST, "Invalid sort");
        }
        String field = parts[0].trim();
        String direction = parts[1].trim();
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new ELibraryException(ErrorCode.INVALID_REQUEST, "Invalid sort");
        }
        Sort.Direction dir;
        if ("asc".equalsIgnoreCase(direction)) {
            dir = Sort.Direction.ASC;
        } else if ("desc".equalsIgnoreCase(direction)) {
            dir = Sort.Direction.DESC;
        } else {
            throw new ELibraryException(ErrorCode.INVALID_REQUEST, "Invalid sort");
        }
        return Sort.by(dir, field);
    }
}
