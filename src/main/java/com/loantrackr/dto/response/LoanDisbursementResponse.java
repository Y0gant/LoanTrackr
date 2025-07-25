package com.loantrackr.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@Schema(description = "Response object for a successful loan disbursement")
public class LoanDisbursementResponse {

    @Schema(description = "Unique identifier of the loan", example = "2001")
    private Long loanId;

    @Schema(description = "Total amount disbursed to the borrower's account", example = "48000.00")
    private BigDecimal disbursedAmount;

    @Schema(description = "Monthly EMI amount calculated based on disbursed amount, interest, and tenure", example = "4450.00")
    private BigDecimal emiAmount;

    @Schema(description = "Total repayment amount including principal and interest", example = "53400.00")
    private BigDecimal totalAmount;

    @Schema(description = "Total interest payable over the loan period", example = "5400.00")
    private BigDecimal totalInterest;

    @Schema(description = "Date when the first EMI is due", example = "2025-08-15")
    private LocalDate firstDueDate;

    @Schema(description = "Unique transaction ID of the disbursement transfer", example = "TXN202507230001")
    private String disbursementTransactionId;

    @Schema(description = "Human-readable message about the disbursement outcome", example = "Loan disbursed successfully")
    private String message;

}