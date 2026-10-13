package com.library.catalogue.dto;

import com.library.catalogue.model.Book;
import com.library.catalogue.model.BookCopy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDetailResponse {
    private Book book;
    private List<BookCopy> copies;
    private int availableCount;
    private int borrowedCount;
    private int reservedCount;
    private String shareableShortUrl;
}
