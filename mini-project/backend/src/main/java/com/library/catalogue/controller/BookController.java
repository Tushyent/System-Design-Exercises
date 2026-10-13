package com.library.catalogue.controller;

import com.library.catalogue.dto.BookDetailResponse;
import com.library.catalogue.dto.CreateBookRequest;
import com.library.catalogue.model.Book;
import com.library.catalogue.service.BookService;
import com.library.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Book>>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Page<Book> books = bookService.searchBooks(null, PageRequest.of(page, size, sort), "optimized");
        return ResponseEntity.ok(ApiResponse.ok(books, "Books retrieved"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<Book>>> searchBooks(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "optimized") String mode) {

        Page<Book> results = bookService.searchBooks(q, PageRequest.of(page, size), mode);
        return ResponseEntity.ok(ApiResponse.ok(results, "Search completed (" + mode + " mode)"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookDetailResponse>> getBookDetails(@PathVariable String id) {
        BookDetailResponse response = bookService.getBookDetails(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Book details retrieved"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Book>> createBook(@Valid @RequestBody CreateBookRequest request) {
        Book created = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "Book added to catalogue"));
    }

    @PostMapping("/{id}/copies")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Book>> addCopies(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int count) {
        Book updated = bookService.addCopies(id, count);
        return ResponseEntity.ok(ApiResponse.ok(updated, count + " physical copies added"));
    }
}
