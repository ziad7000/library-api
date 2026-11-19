package com.library.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "library")
public record LibraryProperties(
        int maxActiveLoans,
        int loanPeriodDays,
        int maxRenewals,
        BigDecimal dailyFineRate,
        int holdDurationDays
) {
    public LibraryProperties {
        if (maxActiveLoans <= 0) {
            maxActiveLoans = 5;
        }
        if (loanPeriodDays <= 0) {
            loanPeriodDays = 14;
        }
        if (maxRenewals < 0) {
            maxRenewals = 2;
        }
        if (dailyFineRate == null || dailyFineRate.compareTo(BigDecimal.ZERO) < 0) {
            dailyFineRate = new BigDecimal("2.00");
        }
        if (holdDurationDays <= 0) {
            holdDurationDays = 3;
        }
    }
}
