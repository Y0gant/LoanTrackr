package com.loantrackr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LenderPortfolioResponse {
    private String lenderName;
    private long pendingApplications;
    private long approvedApplications;
    private long disbursedLoans;
    private long rejectedApplications;
    private long completedLoans;
    private BigDecimal totalDisbursedAmount;
    private BigDecimal totalOutstandingAmount;
    private BigDecimal totalCollectedAmount;
}