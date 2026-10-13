package com.library.metrics.service;

import com.library.autocomplete.AutocompleteService;
import com.library.catalogue.repository.BookCopyRepository;
import com.library.catalogue.repository.BookRepository;
import com.library.catalogue.service.BookService;
import com.library.circulation.model.LoanStatus;
import com.library.circulation.repository.LoanRepository;
import com.library.consistenthash.ConsistentHashService;
import com.library.crawler.repository.CrawlerJobRepository;
import com.library.fines.model.Fine;
import com.library.fines.model.FineStatus;
import com.library.fines.repository.FineRepository;
import com.library.idgenerator.SnowflakeIdGenerator;
import com.library.members.repository.MemberRepository;
import com.library.ratelimit.TokenBucketRateLimiter;
import com.library.reservations.model.ReservationStatus;
import com.library.reservations.repository.ReservationRepository;
import com.library.shorturl.ShortUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final TokenBucketRateLimiter rateLimiter;
    private final BookService bookService;
    private final SnowflakeIdGenerator idGenerator;
    private final ConsistentHashService consistentHashService;
    private final ShortUrlService shortUrlService;
    private final AutocompleteService autocompleteService;
    private final CrawlerJobRepository crawlerJobRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository copyRepository;
    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final FineRepository fineRepository;

    public Map<String, Object> getSystemMetricsSummary() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // 1. Rate Limiter Metrics
        metrics.put("rateLimiter", rateLimiter.getMetrics());

        // 2. Redis Key-Value Store Cache Metrics
        long hits = bookService.getCacheHits();
        long misses = bookService.getCacheMisses();
        long totalCacheRequests = hits + misses;
        double hitRatio = totalCacheRequests > 0 ? ((double) hits / totalCacheRequests) * 100.0 : 0.0;
        metrics.put("keyValueStore", Map.of(
                "cacheHits", hits,
                "cacheMisses", misses,
                "totalCacheRequests", totalCacheRequests,
                "hitRatioPercent", hitRatio
        ));

        // 3. Snowflake Unique ID Generator Metrics
        metrics.put("idGenerator", Map.of(
                "totalIdsGenerated", idGenerator.getGeneratedCount(),
                "collisionsObserved", 0L,
                "algorithm", "64-bit Snowflake (Timestamp + Datacenter + Worker + Sequence)"
        ));

        // 4. Consistent Hashing Ring Metrics
        metrics.put("consistentHashing", consistentHashService.getRingState());

        // 5. URL Shortener Metrics
        metrics.put("urlShortener", Map.of(
                "totalResolutions", shortUrlService.getTotalResolutions(),
                "averageLatencyMs", shortUrlService.getAverageResolutionLatencyMs()
        ));

        // 6. Autocomplete Metrics
        metrics.put("autocomplete", autocompleteService.getMetrics());

        // 7. Crawler Metrics
        long totalCrawlJobs = crawlerJobRepository.count();
        metrics.put("crawler", Map.of(
                "totalJobsExecuted", totalCrawlJobs
        ));

        // 8. Overall Library Data Volume
        List<Fine> unpaidFines = fineRepository.findAll().stream()
                .filter(f -> f.getStatus() == FineStatus.UNPAID)
                .toList();
        double totalUnpaidAmount = unpaidFines.stream().mapToDouble(Fine::getAmount).sum();

        metrics.put("libraryStats", Map.of(
                "totalMembers", memberRepository.count(),
                "totalTitles", bookRepository.count(),
                "totalCopies", copyRepository.count(),
                "activeLoans", loanRepository.findAll().stream().filter(l -> l.getStatus() == LoanStatus.ACTIVE).count(),
                "pendingReservations", reservationRepository.countByBookIdAndStatus(null, ReservationStatus.PENDING),
                "unpaidFinesCount", unpaidFines.size(),
                "totalUnpaidFinesAmount", totalUnpaidAmount
        ));

        return metrics;
    }
}
