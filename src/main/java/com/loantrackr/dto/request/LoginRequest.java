package com.loantrackr.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User login request payload")
public class LoginRequest {

    @Schema(
            description = "Username or email address for authentication",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED,
            maxLength = 100
    )
    @NotBlank(message = "Username or email is required")
    private String identifier;

    @Schema(
            description = "User password for authentication",
            example = "mySecurePassword123",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 8,
            maxLength = 50,
            format = "password"
    )
    @NotBlank(message = "password is required")
    private String password;
}
