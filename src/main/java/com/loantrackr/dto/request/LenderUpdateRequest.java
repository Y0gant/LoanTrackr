package com.loantrackr.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LenderUpdateRequest {

    @Pattern(
            regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
            message = "Invalid GSTIN format"
    )
    @Schema(
            description = "Valid 15-character GSTIN format (e.g., 27ABCDE1234F1Z5)",
            example = "27ABCDE1234F1Z5"
    )
    private String gstin;

    @NotBlank(message = "RBI License Number is mandatory")
    @Size(min = 6, max = 20, message = "RBI License Number must be between 6 and 20 characters")
    @Schema(
            description = "RBI license number of the lender (6–20 characters)",
            example = "RBI123456"
    )
    private String rbiLicenseNumber;

    @NotBlank(message = "Organization name is required")
    @Size(min = 3, max = 100, message = "Organization name must be between 3 and 100 characters")
    @Schema(
            description = "Registered name of the lending organization",
            example = "MicroFin Capital Pvt. Ltd."
    )
    private String organizationName;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "1.0", inclusive = true, message = "Interest rate must be at least 1%")
    @DecimalMax(value = "50.0", inclusive = true, message = "Interest rate must not exceed 50%")
    @Schema(
            description = "Annual interest rate (%) charged on loans",
            example = "12.5"
    )
    private BigDecimal interestRate;

    @NotBlank(message = "Supported tenures must not be blank")
    @Pattern(
            regexp = "^([1-9][0-9]*)(,[1-9][0-9]*)*$",
            message = "Supported tenures must be a comma-separated list of positive integers (e.g., 6,12,24)"
    )
    @Schema(
            description = "Comma-separated list of supported loan tenures in months",
            example = "6,12,24"
    )
    private String supportedTenures;

    @Schema(
            description = "Loan processing fees (optional, in absolute currency value)",
            example = "500.00"
    )
    private BigDecimal processingFees;
}