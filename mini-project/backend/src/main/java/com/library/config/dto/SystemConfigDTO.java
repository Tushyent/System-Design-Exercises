package com.library.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigDTO {
    @Builder.Default
    private int loanPeriodDays = 14;

    @Builder.Default
    private int defaultBorrowLimit = 5;

    @Builder.Default
    private double dailyFineRate = 1.50;

    @Builder.Default
    private int holdExpiryHours = 48;

    @Builder.Default
    private boolean maintenanceMode = false;

    @Builder.Default
    private int maxRenewalCount = 2;
}
