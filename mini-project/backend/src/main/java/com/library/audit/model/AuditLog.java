package com.library.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    @Indexed
    private String staffEmail;

    private String staffRole;

    @Indexed
    private String action; // CHECKOUT, RETURN, FINE_WAIVED, FINE_ADJUSTED, CASH_PAYMENT, MEMBER_REGISTER, MEMBER_UPDATE, MEMBER_SUSPEND, BOOK_UPDATE, BOOK_DELETE, COPY_UPDATE, COPY_DELETE, CONFIG_UPDATE, STAFF_CREATE, ROLE_CHANGE

    private String targetResource; // MEMBER, BOOK, COPY, LOAN, FINE, CONFIG, STAFF

    @Indexed
    private String targetId;

    private String details;

    @Builder.Default
    @Indexed
    private Instant timestamp = Instant.now();
}
