package com.loantrackr.dto.request;


import com.loantrackr.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User registration request payload")
public class RegisterUser {

    @Schema(
            description = "Unique username for the user account",
            example = "john_doe123",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 3,
            maxLength = 50
    )
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Schema(
            description = "Valid email address for user communication and login",
            example = "john.doe@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED,
            format = "email"
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Schema(
            description = "Strong password meeting security requirements",
            example = "SecurePass123!",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 10,
            format = "password",
            pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{10,}$"
    )
    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{10,}$",
            message = "Password must be at least 10 characters long and include one uppercase, one lowercase, one number, and one special character"
    )
    private String password;

    @Schema(
            description = "User role determining access permissions and capabilities",
            example = "BORROWER",
            defaultValue = "BORROWER",
            allowableValues = {
                    "SYSTEM_ADMIN",
                    "LENDER",
                    "LOAN_MANAGER",
                    "LOAN_OFFICER",
                    "BORROWER"
            }
    )
    private Role role;

}
