package com.loantrackr.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "JWT authentication response containing access token and user information")
public class JwtAuthResponse {

    @Schema(
            description = "JWT access token for authenticated requests",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
            requiredMode = Schema.RequiredMode.AUTO
    )
    private String accessToken;

    @Schema(
            description = "Token type for authorization header",
            example = "Bearer",
            defaultValue = "Bearer",
            requiredMode = Schema.RequiredMode.AUTO
    )
    private String tokenType = "Bearer";

    @Schema(
            description = "Authenticated user information",
            requiredMode = Schema.RequiredMode.AUTO
    )
    private UserResponse user;
}