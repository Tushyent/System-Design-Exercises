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
}
