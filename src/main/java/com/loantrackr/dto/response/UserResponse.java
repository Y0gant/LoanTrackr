package com.loantrackr.dto.response;

import com.loantrackr.enums.AuthProvider;
import com.loantrackr.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User information response containing profile details and account status")
public class UserResponse {

    @Schema(
            description = "Unique identifier for the user",
            example = "12345",
            requiredMode = Schema.RequiredMode.AUTO
    )
    private Long id;

    @Schema(
            description = "User's unique username",
            example = "john_doe123",
            requiredMode = Schema.RequiredMode.AUTO
    )
    private String username;

    @Schema(
            description = "User's email address",
            example = "john.doe@example.com",
            format = "email",
            requiredMode = Schema.RequiredMode.AUTO
    )
    private String email;

    @Schema(
            description = "User's role determining access permissions",
            example = "BORROWER",
            requiredMode = Schema.RequiredMode.AUTO,
            allowableValues = {
                    "SYSTEM_ADMIN",
                    "LENDER",
                    "LOAN_MANAGER",
                    "LOAN_OFFICER",
                    "BORROWER"
            }
    )
    private Role role;

    @Schema(
            description = "Authentication provider used for user registration",
            example = "LOCAL",
            requiredMode = Schema.RequiredMode.AUTO,
            allowableValues = {
                    "LOCAL",
                    "GOOGLE",
                    "FACEBOOK",
                    "GITHUB"
            }
    )
    private AuthProvider provider;

    @Schema(
            description = "Account status - true if user account is active, false if deactivated or suspended",
            example = "true",
            requiredMode = Schema.RequiredMode.AUTO
    )
    private boolean isActive;

    @Schema(
            description = "Email verification status - true if email has been verified, false if pending verification",
            example = "true",
            requiredMode = Schema.RequiredMode.AUTO
    )
    private boolean isEmailVerified;


}