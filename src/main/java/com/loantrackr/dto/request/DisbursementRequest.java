package com.loantrackr.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DisbursementRequest {

    @Schema(
            description = "Unique ID of the approved loan to be disbursed",
            example = "1024"
    )
    private Long loanId;

    @Schema(
            description = "Bank account number of the borrower where funds will be disbursed",
            example = "987654321012"
    )
    private String borrowerAccountNumber;

    @Schema(
            description = "Amount to be disbursed to the borrower (must be less than or equal to sanctioned loan amount)",
            example = "25000.00"
    )
    private BigDecimal amount;
}
