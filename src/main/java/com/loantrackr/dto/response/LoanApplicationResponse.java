package com.loantrackr.dto.response;

import com.loantrackr.enums.LoanStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LoanApplicationResponse {
    private Long applicationId;
    private BigDecimal loanAmount;
    private int tenure;
    private BigDecimal emi;
    private BigDecimal interestRate;
    private BigDecimal processingFee;
    private LoanStatus status;
    private String lenderName;
    private LocalDateTime appliedAt;
    private String purpose;
}
