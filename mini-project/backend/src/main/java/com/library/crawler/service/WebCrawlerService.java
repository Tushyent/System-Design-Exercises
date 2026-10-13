package com.library.crawler.service;

import com.library.catalogue.dto.CreateBookRequest;
import com.library.catalogue.model.Book;
import com.library.catalogue.repository.BookRepository;
import com.library.catalogue.service.BookService;
import com.library.crawler.model.CrawlerJob;
import com.library.crawler.model.ExternalBookDto;
import com.library.crawler.repository.CrawlerJobRepository;
import com.library.idgenerator.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebCrawlerService {

    private final CrawlerJobRepository jobRepository;
    private final BookRepository bookRepository;
    private final BookService bookService;
    private final SnowflakeIdGenerator idGenerator;
    private final RestTemplate restTemplate = new RestTemplate();

    public CrawlerJob startCrawlJob(String sourceUrl) {
        String jobId = idGenerator.nextPrefixedId("CRAWL");

        CrawlerJob job = CrawlerJob.builder()
                .id(idGenerator.nextIdString())
                .jobId(jobId)
                .sourceUrl(sourceUrl)
                .status("RUNNING")
                .startedAt(Instant.now())
                .build();

        CrawlerJob savedJob = jobRepository.save(job);

        // Execute crawl asynchronously
        executeCrawl(savedJob);

        return savedJob;
    }

    @Async
    public void executeCrawl(CrawlerJob job) {
        long startTime = System.currentTimeMillis();
        log.info("Starting crawl job {} for URL {}", job.getJobId(), job.getSourceUrl());

        int fetched = 0;
        int parsed = 0;
        int imported = 0;
        int deduplicated = 0;
        int failed = 0;

        try {
            ResponseEntity<List<ExternalBookDto>> response = restTemplate.exchange(
                    job.getSourceUrl(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            List<ExternalBookDto> books = response.getBody();
            if (books != null) {
                fetched = books.size();
                parsed = books.size();

                for (ExternalBookDto dto : books) {
                    try {
                        Optional<Book> existing = bookRepository.findByIsbn(dto.getIsbn());

                        if (existing.isPresent()) {
                            // Deduplicate & Enrich: Book exists, update external metadata
                            Book b = existing.get();
                            b.setExternalRating(dto.getExternalRating());
                            b.setExternalSource(dto.getExternalSource());
                            if (dto.getDescription() != null && (b.getDescription() == null || b.getDescription().isBlank())) {
                                b.setDescription(dto.getDescription());
                            }
                            b.setUpdatedAt(Instant.now());
                            bookRepository.save(b);
                            bookService.invalidateAvailabilityCache(b.getId());
                            deduplicated++;
                        } else {
                            // New Book: Import to Catalogue
                            CreateBookRequest req = CreateBookRequest.builder()
                                    .title(dto.getTitle())
                                    .isbn(dto.getIsbn())
                                    .authors(dto.getAuthors())
                                    .categories(dto.getCategories())
                                    .publisher(dto.getPublisher())
                                    .publicationYear(dto.getPublicationYear())
                                    .description(dto.getDescription())
                                    .coverUrl(dto.getCoverUrl())
                                    .initialCopies(2)
                                    .build();

                            Book importedBook = bookService.createBook(req);
                            importedBook.setExternalRating(dto.getExternalRating());
                            importedBook.setExternalSource(dto.getExternalSource());
                            bookRepository.save(importedBook);
                            imported++;
                        }
                    } catch (Exception itemEx) {
                        log.error("Failed to process crawled item {}: {}", dto.getIsbn(), itemEx.getMessage());
                        failed++;
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            double throughput = duration > 0 ? (fetched / ((double) duration / 1000.0)) : 0.0;

            job.setStatus("COMPLETED");
            job.setRecordsFetched(fetched);
            job.setRecordsParsed(parsed);
            job.setRecordsImported(imported);
            job.setRecordsDeduplicated(deduplicated);
            job.setRecordsFailed(failed);
            job.setDurationMs(duration);
            job.setThroughputBooksPerSec(throughput);
            job.setFinishedAt(Instant.now());

            log.info("Crawl job {} completed: fetched={}, imported={}, deduplicated={}, failed={}, duration={}ms",
                    job.getJobId(), fetched, imported, deduplicated, failed, duration);

        } catch (Exception e) {
            log.error("Crawl job {} failed: {}", job.getJobId(), e.getMessage());
            long duration = System.currentTimeMillis() - startTime;
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            job.setDurationMs(duration);
            job.setFinishedAt(Instant.now());
        }

        jobRepository.save(job);
    }

    public List<CrawlerJob> getAllJobs() {
        return jobRepository.findAllByOrderByStartedAtDesc();
    }
}
