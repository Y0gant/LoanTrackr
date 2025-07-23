package com.loantrackr.dto.request;

import com.loantrackr.enums.PaymentMethod;
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
public class PaymentGatewayRequest {

    @Schema(
            description = "Payment amount to be processed via the gateway (in INR)",
            example = "1500.00"
    )
    private BigDecimal amount;

    @Schema(
            description = "Selected method of payment",
            example = "UPI"
    )
    private PaymentMethod paymentMethod;

    @Schema(
            description = "Loan ID against which the payment is being made",
            example = "1012"
    )
    private Long loanId;

    @Schema(
            description = "Installment number this payment corresponds to",
            example = "3"
    )
    private int installmentNumber;
}