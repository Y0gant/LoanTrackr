package com.loantrackr.dto.request;

import com.loantrackr.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentGatewayRequest {
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private Long loanId;
    private int installmentNumber;
}
