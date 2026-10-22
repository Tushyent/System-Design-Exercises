package com.library.catalogue.dto;

import com.library.catalogue.model.BookCopy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopyDetailResponse {
    private BookCopy copy;
    private String bookId;
    private String bookTitle;
    private String isbn;
    private String author;
}
