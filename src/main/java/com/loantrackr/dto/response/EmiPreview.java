package com.loantrackr.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Preview of EMI details from a lender before loan approval")
public class EmiPreview {

    @Schema(
            description = "Name of the lending organization offering the EMI terms",
            example = "FinTrust Capital Pvt. Ltd."
    )
    public String organization;

    @Schema(
            description = "Monthly EMI amount (in INR) based on requested loan and tenure",
            example = "2345.67"
    )
    public BigDecimal emi;

    @Schema(
            description = "Total amount payable over the full loan tenure (principal + interest)",
            example = "56296.08"
    )
    public BigDecimal totalPayable;

    @Schema(
            description = "Total interest payable over the loan period",
            example = "6296.08"
    )
    public BigDecimal totalInterest;

    @Schema(
            description = "One-time processing fee charged by the lender",
            example = "500.00"
    )
    public BigDecimal processingFee;
}