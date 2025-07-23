package com.loantrackr.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response payload returned after loan disbursement is processed")
public class DisbursementResponse {

    @Schema(
            description = "Unique identifier for the disbursement transaction",
            example = "TXN123456789"
    )
    private String transactionId;

    @Schema(
            description = "Disbursement status: SUCCESS, FAILED, or PENDING",
            example = "SUCCESS"
    )
    private String status;

    @Schema(
            description = "Amount disbursed to the borrower's account (in INR)",
            example = "25000.00"
    )
    private BigDecimal amount;

    @Schema(
            description = "Reason for disbursement failure, if any",
            example = "Insufficient balance in lender account"
    )
    private String failureReason;

    @Schema(
            description = "Date and time at which the disbursement occurred (server time)",
            example = "2025-07-23T10:15:30"
    )
    private LocalDateTime disbursedAt;
}
