package com.loantrackr.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@Schema(description = "Summary view of a lender's loan offering details")
public class LenderSummaryResponse {

    @Schema(
            description = "Unique identifier of the lender",
            example = "101"
    )
    private Long lenderId;

    @Schema(
            description = "Registered name of the lending organization",
            example = "CapitalGrowth Finance Ltd."
    )
    private String organizationName;

    @Schema(
            description = "Annual interest rate offered by the lender (in %)",
            example = "13.5"
    )
    private BigDecimal interestRate;

    @Schema(
            description = "Flat processing fee charged by the lender (in INR)",
            example = "999.00"
    )
    private BigDecimal processingFee;

    @Schema(
            description = "List of supported loan tenures in months",
            example = "[6, 12, 24]"
    )
    private List<Integer> supportedTenures;
}
