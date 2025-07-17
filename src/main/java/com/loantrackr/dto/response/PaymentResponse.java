package com.loantrackr.dto.response;

import com.loantrackr.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    private Long paymentId;
    private String transactionId;
    private PaymentStatus status;
    private BigDecimal amount;
    private int installmentNumber;
    private BigDecimal remainingAmount;
    private LocalDate nextDueDate;
    private String message;
}
