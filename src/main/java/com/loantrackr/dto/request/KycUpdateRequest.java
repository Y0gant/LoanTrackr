package com.loantrackr.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for updating borrower's KYC details (partial update supported)")
public class KycUpdateRequest {

    @Past(message = "Date of birth must be in the past")
    @Schema(example = "1995-04-12")
    private LocalDate dateOfBirth;

    @Schema(example = "Flat 301, Green Residency")
    private String address;

    @Pattern(regexp = "^\\d{6}$", message = "Pin code must be 6 digits")
    @Schema(example = "411038")
    private String pincode;

    @Schema(example = "Pune")
    private String city;

    @Schema(example = "Maharashtra")
    private String state;

    @Schema(example = "Salaried", allowableValues = {"Salaried", "Self-employed", "Student", "Unemployed"})
    private String employmentType;

    @Pattern(regexp = "^[2-9]{1}[0-9]{11}$", message = "Invalid Aadhaar number")
    @Schema(example = "987654321012")
    private String aadhaarNumber;

    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN number")
    @Schema(example = "ABCDE1234F")
    private String panNumber;
}
