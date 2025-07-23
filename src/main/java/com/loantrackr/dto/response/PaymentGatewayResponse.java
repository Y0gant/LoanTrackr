package com.loantrackr.dto.response;

import com.loantrackr.enums.PaymentMethod;
import com.loantrackr.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Response received from the payment gateway after processing a transaction")
public class PaymentGatewayResponse {

    @Schema(description = "Unique transaction ID assigned by the payment gateway", example = "TXN202507231245")
    private String transactionId;

    @Schema(description = "Final status of the payment transaction", example = "SUCCESS")
    private PaymentStatus status;

    @Schema(description = "Amount processed in the transaction", example = "10000.00")
    private BigDecimal amount;

    @Schema(description = "Mode of payment used by the user", example = "UPI")
    private PaymentMethod paymentMethod;

    @Schema(description = "Failure reason in case of a failed payment", example = "Insufficient funds", nullable = true)
    private String failureReason;

    @Schema(description = "Timestamp when the payment was processed", example = "2025-07-23T14:30:00")
    private LocalDateTime processedAt;
}
