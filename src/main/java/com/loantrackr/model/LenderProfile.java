package com.loantrackr.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class LenderProfile {
    @Id
    private Long id;

    @OneToOne
    private User user;

    private String gstin;
    private String rbiLicenseNumber;
    private String organizationName;
    private boolean isVerified; // Set by SystemAdmin
}
