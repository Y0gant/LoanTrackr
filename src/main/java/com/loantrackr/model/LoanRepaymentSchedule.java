package com.loantrackr.model;

import com.loantrackr.enums.LoanRepaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "loan_repayment_schedule")
public class LoanRepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loan_repayment_seq")
    @SequenceGenerator(name = "loan_repayment_seq", sequenceName = "loan_repayment_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Loan loan;

    @Column(nullable = false)
    private int installmentNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal emiAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal interestAmount;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanRepaymentStatus status;

    @Column(precision = 15, scale = 2)
    private BigDecimal lateFee;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalAmountPaid; // EMI + Late Fee

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = LoanRepaymentStatus.PENDING;
        this.lateFee = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper methods
    public boolean isOverdue() {
        return LocalDate.now().isAfter(dueDate.plusDays(3)); // 3-day grace period
    }

    public boolean isPaid() {
        return status == LoanRepaymentStatus.PAID || status == LoanRepaymentStatus.LATE_PAID;
    }

    public BigDecimal getTotalAmountDue() {
        return emiAmount.add(lateFee != null ? lateFee : BigDecimal.ZERO);
    }
}
