package com.loantrackr.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LenderProfile {
    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId //LenderProfile.id == user.id
    private User user;


    private String gstin;
    private String rbiLicenseNumber;
    private String organizationName;
    private boolean isVerified; // Set by SystemAdmin
}
