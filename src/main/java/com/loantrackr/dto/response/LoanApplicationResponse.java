package com.loantrackr.dto.response;

import com.loantrackr.enums.LoanStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Represents a loan application submitted by a borrower")
public class LoanApplicationResponse {

    @Schema(description = "Unique identifier for the loan application", example = "1001")
    private Long applicationId;

    @Schema(description = "Requested loan amount in INR", example = "50000.00")
    private BigDecimal loanAmount;

    @Schema(description = "Loan tenure in months", example = "12")
    private int tenure;

    @Schema(description = "Calculated Equated Monthly Installment (EMI)", example = "4550.75")
    private BigDecimal emi;

    @Schema(description = "Annual interest rate (%) offered by the lender", example = "13.5")
    private BigDecimal interestRate;

    @Schema(description = "One-time processing fee charged by the lender", example = "999.00")
    private BigDecimal processingFee;

    @Schema(description = "Current status of the loan application", example = "PENDING")
    private LoanStatus status;

    @Schema(description = "Name of the lender organization", example = "CapitalGrowth Finance Ltd.")
    private String lenderName;

    @Schema(description = "Timestamp when the application was submitted", example = "2025-07-23T10:15:30")
    private LocalDateTime appliedAt;

    @Schema(description = "Purpose of the loan", example = "Home Renovation")
    private String purpose;
}