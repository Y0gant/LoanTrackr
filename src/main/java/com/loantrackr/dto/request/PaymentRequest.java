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
public class PaymentRequest {

    @Schema(
            description = "Amount being paid (in INR)",
            example = "2000.00"
    )
    private BigDecimal amount;

    @Schema(
            description = "Payment method used by the borrower",
            example = "NET_BANKING"
    )
    private PaymentMethod paymentMethod;

    @Schema(
            description = "Optional remarks or description for the payment",
            example = "EMI payment for July"
    )
    private String remarks;
}
