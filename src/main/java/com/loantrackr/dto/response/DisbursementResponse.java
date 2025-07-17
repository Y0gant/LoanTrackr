package com.loantrackr.dto.response;

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
public class DisbursementResponse {
    private String transactionId;
    private String status;
    private BigDecimal amount;
    private String failureReason;
    private LocalDateTime disbursedAt;
}
