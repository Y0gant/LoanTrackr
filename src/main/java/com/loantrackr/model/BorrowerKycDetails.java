package com.loantrackr.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity

public class BorrowerKycDetails {
    @Id
    private Long id;

    @OneToOne
    private User user;

    private String aadhaarNumber;
    private String panNumber;
    private String driverLicenseNumber;
    private boolean isKycVerified;
}
