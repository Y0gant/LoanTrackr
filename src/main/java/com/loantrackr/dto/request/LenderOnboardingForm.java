package com.loantrackr.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class LenderOnboardingForm {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 10, message = "Password must be at least 10 characters long")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{10,}$",
            message = "Password must contain at least 1 uppercase, 1 lowercase, 1 digit, and 1 special character"
    )
    private String password;

    @NotBlank(message = "GSTIN is required")
    private String gstin;

    @NotBlank(message = "RBI License Number is required")
    private String rbiLicenseNumber;

    @NotBlank(message = "Organization name is required")
    private String organizationName;

    @NotBlank(message = "Contact person name is required")
    private String contactPersonName;

    private MultipartFile panCard;
    private MultipartFile gstCertificate;
    private MultipartFile rbiLicense;
}
