package com.loantrackr.model;

import com.loantrackr.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(optional = false)
    private LenderProfile lender;

    @OneToOne(optional = false)
    private User user;

    @Column(nullable = false)
    private BigDecimal loanRequested;

    @Column(nullable = false)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private BigDecimal processingFee;

    @Column(nullable = false)
    private int tenure;

    @Column(nullable = false)
    private BigDecimal emiAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(nullable = false)
    private String purpose;

    @Column(nullable = false)
    private String incomeSource;

    @Column(nullable = false)
    private BigDecimal monthlyIncome;

    @OneToOne(mappedBy = "loanApplication")
    private Loan loan;

    private LocalDateTime appliedAt;

    @Column
    private LocalDateTime closedAt;

    @PrePersist
    public void prePersist() {
        this.appliedAt = LocalDateTime.now();
        if (this.status == null) this.status = LoanStatus.PENDING;

        if (this.loanRequested != null && this.processingFee != null) {
            this.loanRequested = loanRequested.subtract(processingFee);
        }
    }
}
