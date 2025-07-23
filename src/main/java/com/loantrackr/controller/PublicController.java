package com.loantrackr.controller;

import com.loantrackr.dto.request.RegisterUser;
import com.loantrackr.dto.response.ApiResponse;
import com.loantrackr.dto.response.UserResponse;
import com.loantrackr.exception.SetupLockedException;
import com.loantrackr.model.User;
import com.loantrackr.service.SystemAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/public")
public class PublicController {

    private final SystemAdminService adminService;

    @GetMapping("/bootstrap/admin/start")
    public ResponseEntity<ApiResponse<Boolean>> generateOtp() {
        try {
            log.info("Attempting to generate bootstrap OTP");

            boolean isOtpGenerated = adminService.generateAndSendBootstrapOtp();

            if (!isOtpGenerated) {
                log.error("Failed to generate/send OTP to system email");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Unable to generate/send OTP to system email"));
            }

            log.info("Bootstrap OTP generated successfully");
            return ResponseEntity.ok(ApiResponse.success(true, "OTP sent successfully"));

        } catch (SetupLockedException e) {
            log.warn("Setup already locked: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("System setup is already completed"));

        } catch (IllegalStateException e) {
            log.error("Invalid state during OTP generation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error during OTP generation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    @PostMapping("/bootstrap/admin/register")
    public ResponseEntity<ApiResponse<UserResponse>> bootstrapProject(
            @Valid @RequestBody RegisterUser lenderToRegister,
            @RequestParam String otp) {

        try {
            log.info("Attempting to bootstrap system admin with email: {}",
                    lenderToRegister.getEmail());

            if (adminService.systemAdminExists()) {
                log.warn("Attempt to create admin when one already exists");
                throw new SetupLockedException("System admin already exists");
            }

            User initialSystemAdmin = adminService.createInitialSystemAdmin(lenderToRegister, otp);
            UserResponse userResponse = mapToUserResponse(initialSystemAdmin);

            log.info("System admin created successfully with ID: {}", initialSystemAdmin.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(userResponse, "System admin created successfully"));

        } catch (SetupLockedException e) {
            log.warn("Setup locked: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("System setup is already completed"));

        } catch (IllegalArgumentException e) {
            log.error("Invalid registration data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid registration data: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error during admin registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Registration failed due to an unexpected error"));
        }
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .isActive(user.isActive())
                .isEmailVerified(user.isEmailVerified())
                .provider(user.getProvider())
                .build();
    }
}
