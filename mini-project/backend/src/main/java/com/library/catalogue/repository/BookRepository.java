package com.library.catalogue.repository;

import com.library.catalogue.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends MongoRepository<Book, String> {

    Optional<Book> findByIsbn(String isbn);

    Page<Book> findByCategoriesContainingIgnoreCase(String category, Pageable pageable);

    // Optimized text or regex search
    @Query("{ '$or': [ " +
            "{ 'title': { '$regex': ?0, '$options': 'i' } }, " +
            "{ 'authors': { '$regex': ?0, '$options': 'i' } }, " +
            "{ 'categories': { '$regex': ?0, '$options': 'i' } }, " +
            "{ 'isbn': { '$regex': ?0, '$options': 'i' } } " +
            "] }")
    Page<Book> searchBooks(String regexQuery, Pageable pageable);

    List<Book> findTop10ByOrderByAvailableCopiesDesc();
}
