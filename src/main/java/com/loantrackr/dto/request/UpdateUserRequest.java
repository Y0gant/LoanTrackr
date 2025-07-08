package com.loantrackr.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
    private String username;

    @Email(message = "Invalid email format")
    @Size(max = 100)
    private String email;

    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{10,}$",
            message = "Password must be at least 10 characters long and include one uppercase, one lowercase, one number, and one special character"
    )
    private String password;

    private Boolean isActive;
}
