package com.loantrackr.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {

    @NotNull
    @Positive
    @Schema(
            description = "Requested loan amount in INR (must be a positive number)",
            example = "250000"
    )
    private BigDecimal loanAmount;

    @NotBlank
    @Schema(
            description = "Purpose for which the loan is being requested",
            example = "Home Renovation"
    )
    private String purpose;

    @Min(3)
    @Max(84)
    @Schema(
            description = "Loan tenure in months (minimum 3, maximum 84)",
            example = "24"
    )
    private int tenureInMonths;

    @NotBlank
    @Schema(
            description = "Primary source of applicant's income",
            example = "Freelancing"
    )
    private String incomeSource;

    @Positive
    @Schema(
            description = "Monthly income of the applicant in INR",
            example = "45000"
    )
    private BigDecimal monthlyIncome;
}

