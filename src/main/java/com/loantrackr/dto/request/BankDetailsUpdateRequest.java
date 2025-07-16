package com.loantrackr.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BankDetailsUpdateRequest {

    private String accountHolderName;

    @Pattern(regexp = "\\d{9,20}", message = "Account number must be 9 to 20 digits")
    private String accountNumber;

    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC format")
    private String ifscCode;

    private String bankName;

    private String branchName;

    @Pattern(regexp = "^[\\w.-]+@[\\w.-]+$", message = "Invalid UPI ID format")
    private String upiId;
}
