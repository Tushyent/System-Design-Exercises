package com.library.circulation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "loans")
@CompoundIndex(name = "member_status_idx", def = "{'memberId': 1, 'status': 1}")
public class Loan {

    @Id
    private String id;

    @Indexed(unique = true)
    private String loanId;

    @Indexed
    private String memberId;

    private String memberCode;

    @Indexed
    private String bookId;

    private String bookTitle;

    @Indexed
    private String copyId;

    private String barcode;

    private Instant borrowedAt;
    private Instant dueDate;
    private Instant returnedAt;

    @Builder.Default
    private LoanStatus status = LoanStatus.ACTIVE;

    @Builder.Default
    private boolean fineCalculated = false;

    @Builder.Default
    private int renewalCount = 0;

    private Instant lastRenewedAt;
}
