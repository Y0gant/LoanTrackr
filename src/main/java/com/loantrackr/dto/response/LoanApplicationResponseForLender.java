package com.loantrackr.dto.response;

import com.loantrackr.enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationResponseForLender {
    private Long applicationId;
    private BigDecimal loanAmount;
    private Integer tenure;
    private BigDecimal emi;
    private BigDecimal interestRate;
    private BigDecimal processingFee;
    private LoanStatus status;
    private String lenderName;
    private LocalDateTime appliedAt;
    private String purpose;

    private String borrowerName;
    private String borrowerEmail;
    private BigDecimal monthlyIncome;
    private String incomeSource;


}