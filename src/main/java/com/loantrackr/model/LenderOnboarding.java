package com.loantrackr.model;

import com.loantrackr.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lender_onboarding_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LenderOnboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String gstin;

    @Column(unique = true, nullable = false)
    private String rbiLicenseNumber;

    @Column(nullable = false)
    private String organizationName;

    @Column(nullable = false)
    private String contactPersonName;

    @Builder.Default
    private boolean reviewed = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    private String rejectionReason;

    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    private String gstCertificate; // File path or URL
    private String panCard;
    private String rbiLicense;

    @PrePersist
    public void setTimestamps() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
    }
}
