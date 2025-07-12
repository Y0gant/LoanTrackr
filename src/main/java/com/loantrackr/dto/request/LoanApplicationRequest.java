package com.loantrackr.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class LoanApplicationRequest {
    @NotNull
    @Positive
    private BigDecimal loanAmount;

    @NotBlank
    private String purpose;

    @Min(3)
    @Max(84)
    private int tenureInMonths;

    @NotBlank
    private String incomeSource;

    @Positive
    private BigDecimal monthlyIncome;

}
