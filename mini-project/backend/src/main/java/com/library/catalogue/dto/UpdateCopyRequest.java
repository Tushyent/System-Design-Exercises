package com.library.catalogue.dto;

import com.library.catalogue.model.CopyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCopyRequest {
    private CopyStatus status;
    private String barcode;
    private String location;
    private String conditionNotes;
}
