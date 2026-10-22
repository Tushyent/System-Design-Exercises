package com.library.catalogue.controller;

import com.library.catalogue.dto.BookDetailResponse;
import com.library.catalogue.dto.CreateBookRequest;
import com.library.catalogue.model.Book;
import com.library.catalogue.service.BookService;
import com.library.common.dto.ApiResponse;
import com.library.common.exception.RateLimitExceededException;
import com.library.ratelimit.RateLimiterDecision;
import com.library.ratelimit.TokenBucketRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    /**
     * Public read policy (see ratelimit.search.* in application.properties):
     * generous burst capacity so normal browsing and load-testing are not
     * throttled, while runaway scrapers are still capped.
     */
    @Value("${ratelimit.search.capacity:1500}")
    private int readBurstCapacity;

    @Value("${ratelimit.search.refill-per-second:750}")
    private int readRefillPerSecond;

    private final BookService bookService;
    private final TokenBucketRateLimiter rateLimiter;

    private void enforceReadRateLimit(HttpServletRequest httpRequest) {
        RateLimiterDecision decision = rateLimiter.tryAcquire(
                "search:" + httpRequest.getRemoteAddr(), readBurstCapacity, readRefillPerSecond);
        if (!decision.isAllowed()) {
            throw new RateLimitExceededException(
                    "Catalogue read rate limit exceeded. Please slow down.", decision.getRetryAfterSeconds());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<Book>>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest httpRequest) {

        enforceReadRateLimit(httpRequest);
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Page<Book> books = bookService.searchBooks(null, PageRequest.of(page, size, sort), "optimized");
        return ResponseEntity.ok(ApiResponse.ok(books, "Books retrieved"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<Book>>> searchBooks(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "optimized") String mode,
            HttpServletRequest httpRequest) {

        enforceReadRateLimit(httpRequest);
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

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Book>> updateBook(
            @PathVariable String id,
            @RequestBody com.library.catalogue.dto.UpdateBookRequest request,
            Authentication authentication) {
        String staffEmail = authentication != null ? authentication.getName() : "staff";
        Book updated = bookService.updateBook(id, request, staffEmail);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Book updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBook(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean hardDelete,
            Authentication authentication) {
        String staffEmail = authentication != null ? authentication.getName() : "staff";
        bookService.deleteOrArchiveBook(id, hardDelete, staffEmail);
        return ResponseEntity.ok(ApiResponse.ok(null, hardDelete ? "Book deleted" : "Book archived"));
    }

    @PostMapping("/{id}/copies")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Book>> addCopies(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") int count) {
        Book updated = bookService.addCopies(id, count);
        return ResponseEntity.ok(ApiResponse.ok(updated, count + " physical copies added"));
    }

    @GetMapping("/{id}/copies")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<com.library.catalogue.model.BookCopy>>> getBookCopies(@PathVariable String id) {
        List<com.library.catalogue.model.BookCopy> copies = bookService.getCopiesByBookId(id);
        return ResponseEntity.ok(ApiResponse.ok(copies, "Copies retrieved"));
    }

    @GetMapping("/copies/all")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<com.library.catalogue.dto.CopyDetailResponse>>> getAllCopiesDetailed(
            @RequestParam(required = false) String bookId,
            @RequestParam(required = false) com.library.catalogue.model.CopyStatus status,
            @RequestParam(required = false) String barcode) {
        List<com.library.catalogue.dto.CopyDetailResponse> list = bookService.getAllCopiesDetailed(bookId, status, barcode);
        return ResponseEntity.ok(ApiResponse.ok(list, "Inventory copies retrieved"));
    }

    @PutMapping("/copies/{copyId}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<com.library.catalogue.model.BookCopy>> updateCopy(
            @PathVariable String copyId,
            @RequestBody com.library.catalogue.dto.UpdateCopyRequest request,
            Authentication authentication) {
        String staffEmail = authentication != null ? authentication.getName() : "staff";
        com.library.catalogue.model.BookCopy updated = bookService.updateCopy(copyId, request, staffEmail);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Copy updated successfully"));
    }

    @DeleteMapping("/copies/{copyId}")
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCopy(
            @PathVariable String copyId,
            Authentication authentication) {
        String staffEmail = authentication != null ? authentication.getName() : "staff";
        bookService.deleteCopy(copyId, staffEmail);
        return ResponseEntity.ok(ApiResponse.ok(null, "Copy deleted"));
    }
}
