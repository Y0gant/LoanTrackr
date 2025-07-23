package com.loantrackr.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class LenderOnboardingForm {

    @NotBlank(message = "Username is required")
    @Schema(
            description = "Desired username for lender's admin account",
            example = "lender_admin"
    )
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(
            description = "Email address of the lender admin",
            example = "admin@nbfclender.com"
    )
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 10, message = "Password must be at least 10 characters long")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{10,}$",
            message = "Password must contain at least 1 uppercase, 1 lowercase, 1 digit, and 1 special character"
    )
    @Schema(
            description = "Secure password with minimum 10 characters including uppercase, lowercase, digit, and special character",
            example = "StrongPass@123"
    )
    private String password;

    @NotBlank(message = "GSTIN is required")
    @Schema(
            description = "GST Identification Number of the lending institution",
            example = "27ABCDE1234F1Z5"
    )
    private String gstin;

    @NotBlank(message = "RBI License Number is required")
    @Schema(
            description = "RBI-issued license number for the lender",
            example = "RBI123456789"
    )
    private String rbiLicenseNumber;

    @NotBlank(message = "Organization name is required")
    @Schema(
            description = "Legal name of the lender organization",
            example = "FinTrust Capital Pvt. Ltd."
    )
    private String organizationName;

    @NotBlank(message = "Contact person name is required")
    @Schema(
            description = "Full name of the primary point of contact for this lender",
            example = "Amit Desai"
    )
    private String contactPersonName;

    @Schema(
            description = "Upload scanned copy of PAN card (PDF or image)",
            type = "string",
            format = "binary"
    )
    private MultipartFile panCard;

    @Schema(
            description = "Upload GST certificate (PDF or image)",
            type = "string",
            format = "binary"
    )
    private MultipartFile gstCertificate;

    @Schema(
            description = "Upload RBI license (PDF or image)",
            type = "string",
            format = "binary"
    )
    private MultipartFile rbiLicense;
}
