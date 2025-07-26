package com.loantrackr.controller;

import com.loantrackr.dto.request.LenderOnboardingForm;
import com.loantrackr.dto.request.LoginRequest;
import com.loantrackr.dto.request.RegisterBorrowerRequest;
import com.loantrackr.dto.request.RegisterUser;
import com.loantrackr.dto.response.ApiResponse;
import com.loantrackr.dto.response.UserResponse;
import com.loantrackr.exception.OperationNotAllowedException;
import com.loantrackr.exception.SetupLockedException;
import com.loantrackr.model.LenderOnboarding;
import com.loantrackr.model.User;
import com.loantrackr.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/public")
public class PublicController {

    private final SystemAdminService adminService;
    private final OtpService otpService;
    private final BorrowerService borrowerService;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final LenderProfileService lenderProfileService;


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> authenticate(@RequestBody @Valid LoginRequest request) {
        try {
            log.info("trying to login user and generate JWT token");
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getIdentifier(), request.getPassword()));
            String jwt = userService.generateJwtForLogin(request);
            return ResponseEntity.ok(ApiResponse.success(jwt, "User login successful"));
        } catch (Exception e) {
            log.error("Unexpected error during user login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

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

    @GetMapping("/borrower/otp")
    public ResponseEntity<ApiResponse<Object>> getBorrowerOtp(@RequestParam @Email String email) {
        try {
            log.info("Attempting to generate OTP for borrower's email :{}", email);

            boolean otpGenerated = otpService.generateAndSendOtp(email);
            if (!otpGenerated) {
                log.error("Failed to generate/send OTP to email :{}", email);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Unable to generate/send OTP to system email"));
            }
            log.info("OTP generated successfully for borrower email :{}", email);
            return ResponseEntity.ok(ApiResponse.success(true, "OTP sent successfully"));
        } catch (IllegalStateException e) {
            log.error("Invalid state during OTP generation : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error during OTP generation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    @PostMapping("/borrower")
    public ResponseEntity<ApiResponse<Object>> createBorrower(@Valid @RequestBody RegisterBorrowerRequest userToRegister, @RequestParam String otp) {
        try {
            log.info("Attempting to create borrower with username :{} and email :{}", userToRegister.getUsername(), userToRegister.getEmail());
            boolean emailVerified = otpService.validateOtp(userToRegister.getEmail(), otp);
            if (!emailVerified) {
                log.warn("Failed to verify otp for borrower with email :{}", userToRegister.getEmail());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Unable to verify OTP for email :" + userToRegister.getEmail()));
            }
            UserResponse borrower = borrowerService.createBorrower(userToRegister);
            return ResponseEntity.ok(ApiResponse.success(borrower, "Account created successfully"));
        } catch (IllegalStateException e) {
            log.error("Invalid state during borrower creation : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during borrower creation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    @PostMapping(value = "/lender/application", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Object>> lenderApplication(@Valid @ModelAttribute LenderOnboardingForm form) {
        try {
            LenderOnboarding lenderOnboardingApplication = lenderProfileService.createLenderOnboardingApplication(form);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(lenderOnboardingApplication, "Application submitted successfully"));
        } catch (OperationNotAllowedException e) {
            log.error("Lender Application has a duplicate email");
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Try different email"));
        } catch (Exception e) {
            log.error("Unexpected error while submitting lender application", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred"));
        }

    }

}
