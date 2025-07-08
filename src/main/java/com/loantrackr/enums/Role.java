package com.loantrackr.enums;

public enum Role {
    SYSTEM_ADMIN,   // Full control: verifies lenders, deletes users, manages platform
    LENDER,         // Verified financial entity (Bank, NBFC, Microfinance)
    LOAN_MANAGER,   // Mid-level admin for a LENDER, manages officers and loans
    LOAN_OFFICER,   // Executes loans, handles borrowers under LENDER
    BORROWER        // Public users who apply for loans
}
