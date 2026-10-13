package com.library.shorturl;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortUrlRepository extends MongoRepository<ShortUrl, String> {
    Optional<ShortUrl> findByShortCode(String shortCode);
    Optional<ShortUrl> findByResourceTypeAndResourceId(String resourceType, String resourceId);
}
