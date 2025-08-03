package com.loantrackr.dto.response;

import com.loantrackr.enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDetailsResponse {
    private Long loanId;
    private String borrowerName;
    private BigDecimal principalAmount;
    private BigDecimal totalAmountToRepay;
    private BigDecimal remainingAmount;
    private BigDecimal totalInterestAmount;
    private Integer totalInstallments;
    private Integer paidInstallments;
    private LocalDate nextDueDate;
    private LoanStatus status;
    private LocalDateTime disbursedAt;
    private boolean isFullyRepaid;
}