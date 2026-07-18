package com.example.urlshortener_red.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.urlshortener_red.model.URLModel;

@Repository
public interface URLRepo extends MongoRepository<URLModel, String> {

    // Find existing URL using the long URL
    Optional<URLModel> findByLongUrl(String longUrl);

    // Find URL using the generated short code
    Optional<URLModel> findByShortCode(String shortCode);

    // Check whether a short code already exists
    boolean existsByShortCode(String shortCode);

}