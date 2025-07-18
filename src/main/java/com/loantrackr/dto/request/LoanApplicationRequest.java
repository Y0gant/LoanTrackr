package com.loantrackr.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
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
