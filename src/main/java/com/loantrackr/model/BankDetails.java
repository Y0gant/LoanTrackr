package com.loantrackr.model;

import jakarta.persistence.*;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class BankDetails {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    private User user;

    @Column(nullable = false)
    private String accountHolderName;

    @Column(nullable = false, length = 20)
    private String accountNumber;

    @Column(nullable = false, length = 11)
    private String ifscCode;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String branchName;

    @Column
    private String upiId; // Optional fallback for UPI-based eMandates

    @Column(nullable = false)
    private boolean isAccountVerified = false;

    public BankDetails(User user, String accountHolderName, String accountNumber, String ifscCode, String bankName, String branchName, boolean isAccountVerified) {
        this.user = user;
        this.accountHolderName = accountHolderName;
        this.accountNumber = accountNumber;
        this.ifscCode = ifscCode;
        this.bankName = bankName;
        this.branchName = branchName;
        this.upiId = upiId;
        this.isAccountVerified = isAccountVerified;
    }
}