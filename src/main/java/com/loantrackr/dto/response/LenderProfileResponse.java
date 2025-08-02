package com.loantrackr.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LenderProfileResponse {

    private Long userId;
    private String username;
    private String email;
    private String role;
    private boolean isEmailVerified;
    private boolean isActive;

    private String gstin;
    private String rbiLicenseNumber;
    private String organizationName;
    private boolean isVerified;

    private BigDecimal interestRate;
    private BigDecimal processingFee;

    private String supportedTenures;
}
