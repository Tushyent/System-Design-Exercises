package com.library.crawler.repository;

import com.library.crawler.model.CrawlerJob;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrawlerJobRepository extends MongoRepository<CrawlerJob, String> {
    Optional<CrawlerJob> findByJobId(String jobId);
    List<CrawlerJob> findAllByOrderByStartedAtDesc();
}
