package com.loantrackr.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Borrower user registration and onboarding data")
public class RegisterBorrowerRequest {

    //USER
    @NotBlank
    @Size(min = 3, max = 50)
    @Schema(example = "rahul_123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank
    @Email
    @Schema(example = "rahul@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{10,}$",
            message = "Password must be at least 10 characters long and include one uppercase, one lowercase, one number, and one special character"
    )
    @Schema(example = "SecurePass@123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    //KYC
    @NotNull
    @Past
    @Schema(example = "1995-06-15", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate dateOfBirth;

    @NotBlank
    @Schema(example = "Flat 101, ABC Residency", requiredMode = Schema.RequiredMode.REQUIRED)
    private String address;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Pincode must be a 6-digit number")
    @Schema(example = "411001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String pincode;

    @NotBlank
    @Schema(example = "Pune", requiredMode = Schema.RequiredMode.REQUIRED)
    private String city;

    @NotBlank
    @Schema(example = "Maharashtra", requiredMode = Schema.RequiredMode.REQUIRED)
    private String state;

    @NotBlank
    @Schema(example = "Salaried", allowableValues = {"Salaried", "Self-employed", "Student", "Unemployed"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String employmentType;

    @NotBlank
    @Pattern(regexp = "^[2-9]{1}[0-9]{11}$", message = "Invalid Aadhaar number")
    @Schema(example = "987654321012")
    private String aadhaarNumber;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN format")
    @Schema(example = "ABCDE1234F")
    private String panNumber;

    //BANK
    @NotBlank
    @Schema(example = "Rahul Sharma", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountHolderName;

    @NotBlank
    @Pattern(regexp = "\\d{9,20}", message = "Account number must be 9 to 20 digits")
    @Schema(example = "123456789012", requiredMode = Schema.RequiredMode.REQUIRED)
    private String accountNumber;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC format")
    @Schema(example = "SBIN0001234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ifscCode;

    @NotBlank
    @Schema(example = "State Bank of India", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bankName;

    @NotBlank
    @Schema(example = "Kothrud Branch", requiredMode = Schema.RequiredMode.REQUIRED)
    private String branchName;

    @Pattern(regexp = "^[\\w.-]+@[\\w.-]+$", message = "Invalid UPI ID format")
    @Schema(example = "rahul@upi")
    private String upiId;
}
