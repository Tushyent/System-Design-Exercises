package com.library.members.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberEligibilityResponse {
    private String memberId;
    private String memberName;
    private String status;
    private boolean eligible;
    private int activeLoans;
    private int maxBorrowLimit;
    private double unpaidFinesAmount;
    private String ineligibilityReason;
}
