package com.loantrackr.model;

import com.loantrackr.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loan_seq")
    @SequenceGenerator(name = "loan_seq", sequenceName = "loan_seq", allocationSize = 1)
    private Long id;

    @OneToOne(optional = false)
    private LoanApplication loanApplication;

    @ManyToOne(optional = false)
    private User borrower;

    @ManyToOne(optional = false)
    private LenderProfile lender;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmountToRepay;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal remainingAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalInterestAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(nullable = false)
    private LocalDateTime disbursedAt;

    @Column
    private LocalDateTime fullyRepaidAt;

    @Column(nullable = false)
    private LocalDate nextDueDate;

    @Column(nullable = false)
    private int totalInstallments;

    @Column(nullable = false)
    private int paidInstallments = 0;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LoanRepaymentSchedule> repaymentSchedules;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LoanPayment> payments;

    @PrePersist
    public void onCreate() {
        this.status = LoanStatus.DISBURSED;
        this.disbursedAt = LocalDateTime.now();
        this.remainingAmount = this.totalAmountToRepay;
        this.paidInstallments = 0;
    }

    @PreUpdate
    public void checkRepaymentStatus() {
        if (this.remainingAmount.compareTo(BigDecimal.ZERO) == 0 && this.status != LoanStatus.CLOSED) {
            this.status = LoanStatus.CLOSED;
            this.fullyRepaidAt = LocalDateTime.now();
        }
    }

    // Helper methods
    public boolean isFullyRepaid() {
        return paidInstallments == totalInstallments;
    }

    public int getRemainingInstallments() {
        return totalInstallments - paidInstallments;
    }

    public BigDecimal getCompletionPercentage() {
        if (totalInstallments == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(paidInstallments)
                .divide(BigDecimal.valueOf(totalInstallments), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}