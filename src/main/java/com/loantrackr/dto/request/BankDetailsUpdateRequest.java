package com.loantrackr.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BankDetailsUpdateRequest {

    @Schema(
            description = "Name of the account holder as per bank records",
            example = "Rahul Sharma"
    )
    private String accountHolderName;

    @Pattern(regexp = "\\d{9,20}", message = "Account number must be 9 to 20 digits")
    @Schema(
            description = "Bank account number (9 to 20 digits)",
            example = "123456789012"
    )
    private String accountNumber;

    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC format")
    @Schema(
            description = "Indian Financial System Code (IFSC) – 11 characters",
            example = "HDFC0001234"
    )
    private String ifscCode;

    @Schema(
            description = "Name of the bank",
            example = "HDFC Bank"
    )
    private String bankName;

    @Schema(
            description = "Name of the bank branch",
            example = "MG Road, Pune"
    )
    private String branchName;

    @Pattern(regexp = "^[\\w.-]+@[\\w.-]+$", message = "Invalid UPI ID format")
    @Schema(
            description = "UPI ID (must follow format like name@bank)",
            example = "rahul123@upi"
    )
    private String upiId;
}

