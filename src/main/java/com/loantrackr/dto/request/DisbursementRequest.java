package com.loantrackr.dto.request;

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
    private Long loanId;
    private String borrowerAccountNumber;
    private BigDecimal amount;
}
