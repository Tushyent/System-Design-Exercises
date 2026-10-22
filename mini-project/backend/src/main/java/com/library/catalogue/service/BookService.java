package com.library.catalogue.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.library.autocomplete.AutocompleteSuggestion;
import com.library.autocomplete.PrefixTrie;
import com.library.catalogue.dto.BookDetailResponse;
import com.library.catalogue.dto.CreateBookRequest;
import com.library.catalogue.model.Book;
import com.library.catalogue.model.BookCopy;
import com.library.catalogue.model.CopyStatus;
import com.library.catalogue.repository.BookCopyRepository;
import com.library.catalogue.repository.BookRepository;
import com.library.audit.service.AuditLogService;
import com.library.catalogue.dto.CopyDetailResponse;
import com.library.catalogue.dto.UpdateBookRequest;
import com.library.catalogue.dto.UpdateCopyRequest;
import com.library.common.exception.ConflictException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.idgenerator.SnowflakeIdGenerator;
import com.library.shorturl.ShortUrl;
import com.library.shorturl.ShortUrlRepository;
import com.library.shorturl.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final ShortUrlRepository shortUrlRepository;
    private final PrefixTrie prefixTrie;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    // Cache hit / miss metrics
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong searchCacheHits = new AtomicLong(0);
    private final AtomicLong searchCacheMisses = new AtomicLong(0);

    @PostConstruct
    public void initTrie() {
        try {
            List<Book> all = bookRepository.findAll();
            for (Book b : all) {
                indexInAutocomplete(b);
            }
            log.info("Indexed {} books into autocomplete trie on startup", all.size());
        } catch (Exception e) {
            log.warn("Failed to initialize autocomplete trie: {}", e.getMessage());
        }
    }

    public Book createBook(CreateBookRequest request) {
        String bookId = idGenerator.nextIdString();

        Book book = Book.builder()
                .id(bookId)
                .title(request.getTitle())
                .isbn(request.getIsbn())
                .authors(request.getAuthors() != null ? request.getAuthors() : new ArrayList<>())
                .categories(request.getCategories() != null ? request.getCategories() : new ArrayList<>())
                .publisher(request.getPublisher())
                .publicationYear(request.getPublicationYear())
                .description(request.getDescription())
                .coverUrl(request.getCoverUrl())
                .totalCopies(request.getInitialCopies())
                .availableCopies(request.getInitialCopies())
                .build();

        Book savedBook = bookRepository.save(book);

        // Create individual physical copies
        for (int i = 1; i <= request.getInitialCopies(); i++) {
            String copyId = idGenerator.nextIdString();
            String barcode = "BAR-" + bookId.substring(Math.max(0, bookId.length() - 5)) + "-" + i;
            BookCopy copy = BookCopy.builder()
                    .id(copyId)
                    .bookId(bookId)
                    .barcode(barcode)
                    .status(CopyStatus.AVAILABLE)
                    .build();
            copyRepository.save(copy);
        }

        // Index in Autocomplete Trie
        indexInAutocomplete(savedBook);

        // Generate Base62 Short URL
        generateShortUrlForBook(savedBook);

        return savedBook;
    }

    public BookDetailResponse getBookDetails(String bookId) {
        Book book = null;
        String cacheKey = "cache:book:" + bookId;

        // Try Redis Read-Through Cache
        if (redisTemplate != null) {
            try {
                String cachedJson = redisTemplate.opsForValue().get(cacheKey);
                if (cachedJson != null) {
                    cacheHits.incrementAndGet();
                    book = objectMapper.readValue(cachedJson, Book.class);
                }
            } catch (Exception e) {
                log.warn("Redis read failed for {}: {}", cacheKey, e.getMessage());
            }
        }

        if (book == null) {
            cacheMisses.incrementAndGet();
            book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

            // Write to Redis with 10 minute TTL
            if (redisTemplate != null) {
                try {
                    redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(book), Duration.ofMinutes(10));
                } catch (Exception e) {
                    log.warn("Redis write failed for {}: {}", cacheKey, e.getMessage());
                }
            }
        }

        // Authoritative copy states from MongoDB
        List<BookCopy> copies = copyRepository.findByBookId(bookId);
        int available = 0;
        int borrowed = 0;
        int reserved = 0;

        for (BookCopy copy : copies) {
            if (copy.getStatus() == CopyStatus.AVAILABLE) available++;
            else if (copy.getStatus() == CopyStatus.BORROWED) borrowed++;
            else if (copy.getStatus() == CopyStatus.RESERVED_FOR_PICKUP) reserved++;
        }

        // Retrieve or generate short URL
        ShortUrl shortUrl = shortUrlRepository.findByResourceTypeAndResourceId("BOOK", bookId).orElse(null);
        String shortCode = shortUrl != null ? "/s/" + shortUrl.getShortCode() : "";

        return BookDetailResponse.builder()
                .book(book)
                .copies(copies)
                .availableCount(available)
                .borrowedCount(borrowed)
                .reservedCount(reserved)
                .shareableShortUrl(shortCode)
                .build();
    }

    /**
     * Catalogue search with an explicit experiment switch between:
     *  - "baseline"  : full-collection scan with in-memory filtering (no index usage, no cache).
     *  - "optimized" : regex query restricted to indexed fields (title/authors/categories/isbn)
     *                  plus a 60-second Redis cache-aside layer for repeated searches.
     */
    public Page<Book> searchBooks(String query, Pageable pageable, String mode) {
        if (query == null || query.isBlank()) {
            return bookRepository.findAll(pageable);
        }

        String trimmed = query.trim();

        // ---------------- BASELINE MODE (unindexed full scan) ----------------
        if ("baseline".equalsIgnoreCase(mode)) {
            List<Book> all = bookRepository.findAll();
            String needle = trimmed.toLowerCase();
            List<Book> matches = new ArrayList<>();
            for (Book book : all) {
                if (matchesBaseline(book, needle)) {
                    matches.add(book);
                }
            }
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), matches.size());
            List<Book> pageContent = start >= matches.size()
                    ? List.of()
                    : new ArrayList<>(matches.subList(start, end));
            return new PageImpl<>(pageContent, pageable, matches.size());
        }

        // ---------------- OPTIMIZED MODE (indexed + Redis cache-aside) ----------------
        String cacheKey = "cache:search:" + Integer.toHexString(trimmed.toLowerCase().hashCode())
                + ":p" + pageable.getPageNumber() + ":s" + pageable.getPageSize();
        Page<Book> cachedPage = readSearchFromCache(cacheKey, pageable);
        if (cachedPage != null) {
            return cachedPage;
        }

        searchCacheMisses.incrementAndGet();
        Page<Book> results = bookRepository.searchBooks(Pattern.quote(trimmed), pageable);
        writeSearchToCache(cacheKey, results);
        return results;
    }

    private boolean matchesBaseline(Book book, String needle) {
        if (book.getTitle() != null && book.getTitle().toLowerCase().contains(needle)) {
            return true;
        }
        if (book.getIsbn() != null && book.getIsbn().toLowerCase().contains(needle)) {
            return true;
        }
        if (book.getAuthors() != null && book.getAuthors().stream()
                .anyMatch(a -> a != null && a.toLowerCase().contains(needle))) {
            return true;
        }
        return book.getCategories() != null && book.getCategories().stream()
                .anyMatch(c -> c != null && c.toLowerCase().contains(needle));
    }

    private Page<Book> readSearchFromCache(String cacheKey, Pageable pageable) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson == null) {
                return null;
            }
            JsonNode root = objectMapper.readTree(cachedJson);
            List<Book> content = objectMapper.convertValue(root.get("content"), new TypeReference<List<Book>>() {});
            long total = root.path("totalElements").asLong(content.size());
            searchCacheHits.incrementAndGet();
            return new PageImpl<>(content, pageable, total);
        } catch (Exception e) {
            log.warn("Redis search cache read failed for {}: {}", cacheKey, e.getMessage());
            return null;
        }
    }

    private void writeSearchToCache(String cacheKey, Page<Book> results) {
        if (redisTemplate == null) {
            return;
        }
        try {
            Map<String, Object> payload = Map.of(
                    "content", results.getContent(),
                    "totalElements", results.getTotalElements());
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(payload), Duration.ofSeconds(60));
        } catch (Exception e) {
            log.warn("Redis search cache write failed for {}: {}", cacheKey, e.getMessage());
        }
    }

    public Book addCopies(String bookId, int count) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + bookId));

        int currentTotal = copyRepository.countByBookId(bookId);

        for (int i = 1; i <= count; i++) {
            String copyId = idGenerator.nextIdString();
            String barcode = "BAR-" + bookId.substring(Math.max(0, bookId.length() - 5)) + "-" + (currentTotal + i);
            BookCopy copy = BookCopy.builder()
                    .id(copyId)
                    .bookId(bookId)
                    .barcode(barcode)
                    .status(CopyStatus.AVAILABLE)
                    .build();
            copyRepository.save(copy);
        }

        int newTotal = copyRepository.countByBookId(bookId);
        int newAvailable = copyRepository.countByBookIdAndStatus(bookId, CopyStatus.AVAILABLE);

        book.setTotalCopies(newTotal);
        book.setAvailableCopies(newAvailable);
        book.setUpdatedAt(Instant.now());
        Book updated = bookRepository.save(book);

        invalidateAvailabilityCache(bookId);
        return updated;
    }

    public void invalidateAvailabilityCache(String bookId) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete("cache:book:" + bookId);
                redisTemplate.delete("cache:availability:" + bookId);
            } catch (Exception e) {
                log.warn("Redis invalidation failed for book {}: {}", bookId, e.getMessage());
            }
        }
    }

    public void indexInAutocomplete(Book book) {
        // Index title
        prefixTrie.insert(AutocompleteSuggestion.builder()
                .id(book.getId())
                .text(book.getTitle())
                .type("TITLE")
                .score(book.getTotalCopies() * 10)
                .build());

        // Index authors
        if (book.getAuthors() != null) {
            for (String author : book.getAuthors()) {
                prefixTrie.insert(AutocompleteSuggestion.builder()
                        .id(book.getId())
                        .text(author)
                        .type("AUTHOR")
                        .score(book.getTotalCopies() * 5)
                        .build());
            }
        }

        // Index categories
        if (book.getCategories() != null) {
            for (String cat : book.getCategories()) {
                prefixTrie.insert(AutocompleteSuggestion.builder()
                        .id(book.getId())
                        .text(cat)
                        .type("CATEGORY")
                        .score(book.getTotalCopies() * 2)
                        .build());
            }
        }
    }

    private void generateShortUrlForBook(Book book) {
        long codeId = Math.abs(book.getId().hashCode());
        String shortCode = Base62Encoder.encode(codeId);

        // Check if shortCode already exists
        if (shortUrlRepository.findByShortCode(shortCode).isEmpty()) {
            ShortUrl shortUrl = ShortUrl.builder()
                    .shortCode(shortCode)
                    .targetUrl("/books/" + book.getId())
                    .resourceType("BOOK")
                    .resourceId(book.getId())
                    .build();
            shortUrlRepository.save(shortUrl);
        }
    }

    public long getCacheHits() {
        return cacheHits.get();
    }

    public long getCacheMisses() {
        return cacheMisses.get();
    }

    public long getSearchCacheHits() {
        return searchCacheHits.get();
    }

    public long getSearchCacheMisses() {
        return searchCacheMisses.get();
    }

    public void resetCacheMetrics() {
        cacheHits.set(0);
        cacheMisses.set(0);
        searchCacheHits.set(0);
        searchCacheMisses.set(0);
    }

    public Book updateBook(String id, UpdateBookRequest request, String staffEmail) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            book.setTitle(request.getTitle());
        }
        if (request.getIsbn() != null && !request.getIsbn().isBlank()) {
            book.setIsbn(request.getIsbn());
        }
        if (request.getAuthors() != null) {
            book.setAuthors(request.getAuthors());
        }
        if (request.getCategories() != null) {
            book.setCategories(request.getCategories());
        }
        if (request.getPublisher() != null) {
            book.setPublisher(request.getPublisher());
        }
        if (request.getPublicationYear() != null && request.getPublicationYear() > 0) {
            book.setPublicationYear(request.getPublicationYear());
        }
        if (request.getDescription() != null) {
            book.setDescription(request.getDescription());
        }
        if (request.getCoverUrl() != null) {
            book.setCoverUrl(request.getCoverUrl());
        }
        if (request.getArchived() != null) {
            book.setArchived(request.getArchived());
        }
        book.setUpdatedAt(Instant.now());

        Book saved = bookRepository.save(book);
        invalidateAvailabilityCache(saved.getId());
        indexInAutocomplete(saved);

        auditLogService.record(staffEmail, "STAFF", "BOOK_UPDATE", "BOOK", saved.getId(),
                "Updated metadata for book: " + saved.getTitle() + " (ISBN: " + saved.getIsbn() + ")");
        return saved;
    }

    public void deleteOrArchiveBook(String id, boolean hardDelete, String staffEmail) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));

        List<BookCopy> copies = copyRepository.findByBookId(id);
        boolean hasActiveLoans = copies.stream().anyMatch(c -> c.getStatus() == CopyStatus.BORROWED);
        if (hasActiveLoans) {
            throw new ConflictException("Cannot delete or archive book: copies are currently borrowed on loan.");
        }

        if (hardDelete) {
            copyRepository.deleteAll(copies);
            bookRepository.delete(book);
            auditLogService.record(staffEmail, "STAFF", "BOOK_DELETE", "BOOK", id,
                    "Permanently deleted book: " + book.getTitle());
        } else {
            book.setArchived(true);
            book.setUpdatedAt(Instant.now());
            bookRepository.save(book);
            auditLogService.record(staffEmail, "STAFF", "BOOK_ARCHIVE", "BOOK", id,
                    "Archived book title: " + book.getTitle());
        }
        invalidateAvailabilityCache(id);
    }

    public List<BookCopy> getCopiesByBookId(String bookId) {
        return copyRepository.findByBookId(bookId);
    }

    public List<CopyDetailResponse> getAllCopiesDetailed(String bookId, CopyStatus status, String barcodeQuery) {
        List<BookCopy> copies;
        if (bookId != null && !bookId.isBlank()) {
            copies = copyRepository.findByBookId(bookId);
        } else if (barcodeQuery != null && !barcodeQuery.isBlank()) {
            copies = copyRepository.findByBarcodeContainingIgnoreCase(barcodeQuery.trim());
        } else if (status != null) {
            copies = copyRepository.findByStatus(status);
        } else {
            copies = copyRepository.findAll();
        }

        Map<String, Book> bookMap = new java.util.HashMap<>();
        List<CopyDetailResponse> responses = new ArrayList<>();
        for (BookCopy copy : copies) {
            if (status != null && copy.getStatus() != status) {
                continue;
            }
            Book book = bookMap.computeIfAbsent(copy.getBookId(), id -> bookRepository.findById(id).orElse(null));
            responses.add(CopyDetailResponse.builder()
                    .copy(copy)
                    .bookId(copy.getBookId())
                    .bookTitle(book != null ? book.getTitle() : "Unknown Title")
                    .isbn(book != null ? book.getIsbn() : "-")
                    .author(book != null && !book.getAuthors().isEmpty() ? book.getAuthors().get(0) : "-")
                    .build());
        }
        return responses;
    }

    public BookCopy updateCopy(String copyId, UpdateCopyRequest request, String staffEmail) {
        BookCopy copy = copyRepository.findById(copyId)
                .orElseThrow(() -> new ResourceNotFoundException("Copy not found with ID: " + copyId));

        CopyStatus oldStatus = copy.getStatus();
        if (request.getStatus() != null) {
            copy.setStatus(request.getStatus());
        }
        if (request.getBarcode() != null && !request.getBarcode().isBlank()) {
            copy.setBarcode(request.getBarcode());
        }
        if (request.getLocation() != null) {
            copy.setLocation(request.getLocation());
        }
        if (request.getConditionNotes() != null) {
            copy.setConditionNotes(request.getConditionNotes());
        }

        BookCopy saved = copyRepository.save(copy);

        // Recount available copies for parent book
        Book book = bookRepository.findById(saved.getBookId()).orElse(null);
        if (book != null) {
            int newAvailable = copyRepository.countByBookIdAndStatus(book.getId(), CopyStatus.AVAILABLE);
            book.setAvailableCopies(newAvailable);
            bookRepository.save(book);
            invalidateAvailabilityCache(book.getId());
        }

        auditLogService.record(staffEmail, "STAFF", "COPY_UPDATE", "COPY", saved.getBarcode(),
                String.format("Copy %s updated. Status: %s -> %s, Location: %s",
                        saved.getBarcode(), oldStatus, saved.getStatus(), saved.getLocation()));
        return saved;
    }

    public void deleteCopy(String copyId, String staffEmail) {
        BookCopy copy = copyRepository.findById(copyId)
                .orElseThrow(() -> new ResourceNotFoundException("Copy not found with ID: " + copyId));

        if (copy.getStatus() == CopyStatus.BORROWED) {
            throw new ConflictException("Cannot delete copy: currently checked out on loan.");
        }

        copyRepository.delete(copy);

        Book book = bookRepository.findById(copy.getBookId()).orElse(null);
        if (book != null) {
            int newTotal = copyRepository.countByBookId(book.getId());
            int newAvailable = copyRepository.countByBookIdAndStatus(book.getId(), CopyStatus.AVAILABLE);
            book.setTotalCopies(newTotal);
            book.setAvailableCopies(newAvailable);
            bookRepository.save(book);
            invalidateAvailabilityCache(book.getId());
        }

        auditLogService.record(staffEmail, "STAFF", "COPY_DELETE", "COPY", copy.getBarcode(),
                "Deleted physical copy barcode: " + copy.getBarcode());
    }
}
