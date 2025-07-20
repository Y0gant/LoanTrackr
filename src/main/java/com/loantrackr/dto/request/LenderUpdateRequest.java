package com.loantrackr.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LenderUpdateRequest {

    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$", message = "Invalid GSTIN format")
    private String gstin;

    @NotBlank(message = "RBI License Number is mandatory")
    @Size(min = 6, max = 20, message = "RBI License Number must be between 6 and 20 characters")
    private String rbiLicenseNumber;

    @NotBlank(message = "Organization name is required")
    @Size(min = 3, max = 100, message = "Organization name must be between 3 and 100 characters")
    private String organizationName;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "1.0", inclusive = true, message = "Interest rate must be at least 1%")
    @DecimalMax(value = "50.0", inclusive = true, message = "Interest rate must not exceed 50%")
    private BigDecimal interestRate;

    @NotBlank(message = "Supported tenures must not be blank")
    @Pattern(
            regexp = "^([1-9][0-9]*)(,[1-9][0-9]*)*$",
            message = "Supported tenures must be a comma-separated list of positive integers (e.g., 6,12,24)"
    )
    private String supportedTenures;

    private BigDecimal processingFees;
}
