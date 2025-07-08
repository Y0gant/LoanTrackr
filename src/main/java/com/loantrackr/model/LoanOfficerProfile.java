package com.loantrackr.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class LoanOfficerProfile {
    @Id
    private Long id;

    @OneToOne
    private User user;

    private Long createdByUserId;  // Who onboarded them
    private boolean approvedByManager; // Optional
}