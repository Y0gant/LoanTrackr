package com.loantrackr.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LenderProfile {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId //LenderProfile.id == user.id
    private User user;


    @Column(unique = true, nullable = false)
    private String gstin;

    @Column(unique = true, nullable = false)
    private String rbiLicenseNumber;

    @Column(nullable = false)
    private String organizationName;

    private boolean isVerified; // Set by SystemAdmin


    @Column(nullable = false)
    private BigDecimal interestRate; // Annual %

    @Column(nullable = false)
    private BigDecimal processingFee; // Fixed


    @Column(nullable = false)
    private String supportedTenures; // e.g., "6,12,24"

}
