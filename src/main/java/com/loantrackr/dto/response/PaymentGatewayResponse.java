package com.loantrackr.dto.response;

import com.loantrackr.enums.PaymentMethod;
import com.loantrackr.enums.PaymentStatus;
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
public class PaymentGatewayResponse {
    private String transactionId;
    private PaymentStatus status;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String failureReason;
    private LocalDateTime processedAt;
}
