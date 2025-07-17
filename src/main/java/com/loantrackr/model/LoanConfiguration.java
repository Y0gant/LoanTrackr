package com.loantrackr.model;

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
@Table(name = "loan_configuration")
public class LoanConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loan_config_seq")
    @SequenceGenerator(name = "loan_config_seq", sequenceName = "loan_config_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal lateFeeAmount = new BigDecimal("500.00"); // Static ₹500 late fee

    @Column(nullable = false)
    private int gracePeriodDays = 3;

    @Column(nullable = false)
    private int reminderBeforeDueDays = 3;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

