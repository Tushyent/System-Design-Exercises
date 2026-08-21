package com.autocomplete.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String word;
    private int frequency;
}
