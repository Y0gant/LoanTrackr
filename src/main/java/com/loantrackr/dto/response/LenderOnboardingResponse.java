package com.loantrackr.dto.response;


import com.loantrackr.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LenderOnboardingResponse {

    private Long id;

    private String username;
    private String email;
    private String contactPersonName;

    private String organizationName;
    private String gstin;
    private String rbiLicenseNumber;

    private boolean gstCertificate;
    private boolean panCard;
    private boolean rbiLicense;

    private boolean reviewed;
    private RequestStatus status;
    private String rejectionReason;

    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
}

