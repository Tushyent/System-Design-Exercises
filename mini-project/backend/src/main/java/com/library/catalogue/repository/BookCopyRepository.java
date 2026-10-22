package com.library.catalogue.repository;

import com.library.catalogue.model.BookCopy;
import com.library.catalogue.model.CopyStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookCopyRepository extends MongoRepository<BookCopy, String> {
    List<BookCopy> findByBookId(String bookId);
    Optional<BookCopy> findByBarcode(String barcode);
    Optional<BookCopy> findFirstByBookIdAndStatus(String bookId, CopyStatus status);
    List<BookCopy> findByBookIdAndStatus(String bookId, CopyStatus status);
    List<BookCopy> findByStatus(CopyStatus status);
    List<BookCopy> findByBarcodeContainingIgnoreCase(String barcode);
    int countByBookIdAndStatus(String bookId, CopyStatus status);
    int countByBookId(String bookId);
}
