package com.library.members.model;

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
@Document(collection = "members")
public class Member {

    @Id
    private String id;

    @Indexed(unique = true)
    private String memberId;

    private String name;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    @Builder.Default
    private Role role = Role.MEMBER;

    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, SUSPENDED, INACTIVE

    @Builder.Default
    private int maxBorrowLimit = 5;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
