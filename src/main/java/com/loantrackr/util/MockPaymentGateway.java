package com.loantrackr.util;

import com.loantrackr.dto.request.DisbursementRequest;
import com.loantrackr.dto.request.PaymentGatewayRequest;
import com.loantrackr.dto.response.DisbursementResponse;
import com.loantrackr.dto.response.PaymentGatewayResponse;
import com.loantrackr.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class MockPaymentGateway {


    private final Random random = new Random();

    public PaymentGatewayResponse processPayment(PaymentGatewayRequest request) {
        log.info("Processing payment: Amount={}, Method={}, LoanId={}",
                request.getAmount(), request.getPaymentMethod(), request.getLoanId());

        // Simulate processing delay
        try {
            Thread.sleep(1000 + random.nextInt(2000)); // 1-3 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate success/failure (90% success rate)
        boolean isSuccess = random.nextDouble() < 0.9;

        PaymentGatewayResponse response = PaymentGatewayResponse.builder()
                .transactionId(generateGatewayTransactionId())
                .status(isSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .processedAt(LocalDateTime.now())
                .build();

        if (!isSuccess) {
            response.setFailureReason(getRandomFailureReason());
        }

        log.info("Payment processed: TransactionId={}, Status={}",
                response.getTransactionId(), response.getStatus());

        return response;
    }

    public DisbursementResponse processDisbursement(DisbursementRequest request) {
        log.info("Processing disbursement: Amount={}, LoanId={}",
                request.getAmount(), request.getLoanId());


        try {
            Thread.sleep(2000 + random.nextInt(3000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate success (95% success rate for disbursement)
        boolean isSuccess = random.nextDouble() < 0.95;

        DisbursementResponse response = DisbursementResponse.builder()
                .transactionId(generateGatewayTransactionId())
                .status(isSuccess ? "SUCCESS" : "FAILED")
                .amount(request.getAmount())
                .disbursedAt(LocalDateTime.now())
                .build();

        if (!isSuccess) {
            response.setFailureReason("Insufficient funds in lender account");
        }

        log.info("Disbursement processed: TransactionId={}, Status={}",
                response.getTransactionId(), response.getStatus());

        return response;
    }

    private String generateGatewayTransactionId() {
        return "GW" + System.currentTimeMillis() + "_" +
                random.nextInt(100000, 999999);
    }

    private String getRandomFailureReason() {
        String[] reasons = {
                "Insufficient funds",
                "Card declined",
                "Network timeout",
                "Account blocked",
                "Invalid credentials",
                "Transaction limit exceeded"
        };
        return reasons[random.nextInt(reasons.length)];
    }
}
