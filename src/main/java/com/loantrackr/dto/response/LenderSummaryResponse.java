package com.loantrackr.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class LenderSummaryResponse {
    private Long lenderId;
    private String organizationName;
    private BigDecimal interestRate;
    private BigDecimal processingFee;
    private List<Integer> supportedTenures;
}
