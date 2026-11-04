package com.elibrary.controller;

import com.elibrary.api.LoansApi;
import com.elibrary.api.model.LoanResponse;
import com.elibrary.mapper.LibraryMapper;
import com.elibrary.service.LibraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class LoansController implements LoansApi {

    private final LibraryService libraryService;

    public LoansController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @Override
    public ResponseEntity<List<LoanResponse>> listLoans(String xUserId) {
        return ResponseEntity.ok(
                LibraryMapper.toLoanResponses(libraryService.listActiveLoans(xUserId))
        );
    }
}
