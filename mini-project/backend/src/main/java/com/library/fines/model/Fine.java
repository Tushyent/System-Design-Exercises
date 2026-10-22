package com.library.fines.model;

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
@Document(collection = "fines")
@CompoundIndex(name = "member_fine_status_idx", def = "{'memberId': 1, 'status': 1}")
public class Fine {

    @Id
    private String id;

    @Indexed(unique = true)
    private String fineId;

    @Indexed
    private String loanId;

    @Indexed
    private String memberId;

    private int overdueDays;
    private double ratePerDay;
    private double amount;

    @Builder.Default
    private FineStatus status = FineStatus.UNPAID;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant paidAt;
    private String waivedBy;
    private String waiverReason;
    private String paymentMethod; // ONLINE, CASH_OFFLINE, WAIVED
    private String notes;
}
