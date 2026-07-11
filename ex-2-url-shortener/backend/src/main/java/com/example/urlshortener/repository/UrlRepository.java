package com.example.urlshortener.repository;

import com.example.urlshortener.model.UrlModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlRepository extends MongoRepository<UrlModel,String> {
    Optional<UrlModel> findBylongUrl(String longUrl);
    Optional<UrlModel> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);
}
