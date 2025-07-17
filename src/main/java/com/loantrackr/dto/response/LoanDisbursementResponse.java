package com.loantrackr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanDisbursementResponse {
    private Long loanId;
    private BigDecimal disbursedAmount;
    private BigDecimal emiAmount;
    private BigDecimal totalAmount;
    private BigDecimal totalInterest;
    private LocalDate firstDueDate;
    private String disbursementTransactionId;
    private String message;
}
