package com.loantrackr.enums;

public enum Role {
    SYSTEM_ADMIN,   // Full control: verifies lenders, deletes users, manages platform
    LENDER,         // Verified financial entity (Bank, NBFC, Microfinance)
    LOAN_MANAGER,   // Mid-level admin for a LENDER, manages borrowers and loans
    BORROWER        // Public users who apply for loans
}
