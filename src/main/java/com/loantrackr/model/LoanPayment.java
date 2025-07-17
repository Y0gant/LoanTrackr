package com.loantrackr.model;

import com.loantrackr.enums.PaymentMethod;
import com.loantrackr.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "loan_payments")
public class LoanPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loan_payment_seq")
    @SequenceGenerator(name = "loan_payment_seq", sequenceName = "loan_payment_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private LoanRepaymentSchedule repaymentSchedule;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(unique = true, nullable = false)
    private String transactionId;

    @Column
    private String gatewayTransactionId; // From payment gateway

    @Column
    private String failureReason; // For failed payments

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = PaymentStatus.PENDING;
    }

    public boolean isSuccessful() {
        return status == PaymentStatus.SUCCESS;
    }
}

