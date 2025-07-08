package com.loantrackr.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "User update request payload - all fields are optional for partial updates")
public class UpdateUserRequest {

    @Schema(
            description = "Updated username for the user account",
            example = "john_doe_updated",
            minLength = 3,
            maxLength = 50,
            nullable = true
    )
    @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
    private String username;

    @Schema(
            description = "Updated email address for user communication",
            example = "john.updated@example.com",
            format = "email",
            maxLength = 100,
            nullable = true
    )
    @Email(message = "Invalid email format")
    @Size(max = 100)
    private String email;

    @Schema(
            description = "Updated password meeting security requirements",
            example = "NewSecurePass123!",
            format = "password",
            minLength = 10,
            pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{10,}$",
            nullable = true
    )
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{10,}$",
            message = "Password must be at least 10 characters long and include one uppercase, one lowercase, one number, and one special character"
    )
    private String password;

    @Schema(
            description = "User account status - true for active, false for inactive/suspended",
            example = "true",
            nullable = true
    )
    private Boolean isActive;
}
