package com.loantrackr.dto.response;

import com.loantrackr.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response object representing the outcome of a loan payment")
public class PaymentResponse {

    @Schema(description = "Unique identifier for the payment record", example = "98765")
    private Long paymentId;

    @Schema(description = "Transaction ID associated with this payment", example = "TXN202507231512")
    private String transactionId;

    @Schema(description = "Final status of the payment transaction", example = "SUCCESS", enumAsRef = true)
    private PaymentStatus status;

    @Schema(description = "Amount paid in this transaction", example = "2000.00")
    private BigDecimal amount;

    @Schema(description = "Installment number being paid", example = "3")
    private int installmentNumber;

    @Schema(description = "Remaining principal + interest after this payment", example = "12000.00")
    private BigDecimal remainingAmount;

    @Schema(description = "Due date of the next installment, if any", example = "2025-08-23", nullable = true)
    private LocalDate nextDueDate;

    @Schema(description = "Optional message or remarks associated with this payment", example = "Payment received and confirmed")
    private String message;
}
