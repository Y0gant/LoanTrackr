package com.loantrackr.controller;

import com.loantrackr.dto.request.LenderOnboardingForm;
import com.loantrackr.dto.request.LoginRequest;
import com.loantrackr.dto.request.RegisterBorrowerRequest;
import com.loantrackr.dto.request.RegisterUser;
import com.loantrackr.dto.response.ApiResponse;
import com.loantrackr.dto.response.LenderOnboardingResponse;
import com.loantrackr.dto.response.UserResponse;
import com.loantrackr.exception.OperationNotAllowedException;
import com.loantrackr.exception.SetupLockedException;
import com.loantrackr.model.User;
import com.loantrackr.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/public")
@Tag(name = "Public Authentication and Registration",
        description = "Public endpoints for user authentication, admin bootstrap, and account registration")
public class PublicController {

    private final SystemAdminService adminService;
    private final OtpService otpService;
    private final BorrowerService borrowerService;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final LenderProfileService lenderProfileService;


    @PostMapping("/login")
    @Operation(summary = "Authenticate user",
            description = "Authenticates user credentials and returns JWT token for API access")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User authenticated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<ApiResponse<String>> authenticate(
            @Parameter(description = "User login credentials")
            @RequestBody @Valid LoginRequest request) {

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
    @Operation(summary = "Start admin bootstrap process",
            description = "Generates and sends OTP to system email for initial admin account creation")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OTP sent successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "System setup already completed"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Failed to generate/send OTP"
            )
    })
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
    @Operation(summary = "Complete admin bootstrap registration",
            description = "Creates the initial system administrator account using OTP verification")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "System admin created successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid registration data or OTP"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "System setup already completed"
            )
    })
    public ResponseEntity<ApiResponse<UserResponse>> bootstrapProject(
            @Parameter(description = "Admin registration details")
            @Valid @RequestBody RegisterUser lenderToRegister,
            @Parameter(description = "OTP received via email")
            @RequestParam String otp) {

        try {
            log.info("Attempting to bootstrap system admin with email: {}",
                    lenderToRegister.getEmail());

            if (adminService.systemAdminExists()) {
                log.warn("Attempt to create admin when one already exists");
                throw new SetupLockedException("System admin already exists");
            }

            UserResponse initialSystemAdmin = adminService.createInitialSystemAdmin(lenderToRegister, otp);

            log.info("System admin created successfully with ID: {}", initialSystemAdmin.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(initialSystemAdmin, "System admin created successfully"));

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

    @GetMapping("/borrower/otp")
    @Operation(summary = "Generate OTP for borrower registration",
            description = "Sends OTP to email address for borrower account verification")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OTP sent successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Email already exists"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Failed to send OTP"
            )
    })
    public ResponseEntity<ApiResponse<Object>> getBorrowerOtp(
            @Parameter(description = "Email address for OTP delivery")
            @RequestParam @Email String email) {

        try {
            log.info("Attempting to generate OTP for borrower's email :{}", email);

            Optional<User> userByEmail = userService.getUserByEmail(email.toLowerCase());

            if (userByEmail.isPresent()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("User with the provided email already exists, Please try different email"));
            }
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
    @Operation(summary = "Register new borrower account",
            description = "Creates a new borrower account after OTP verification")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Borrower account created successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid registration data"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Invalid or expired OTP"
            )
    })
    public ResponseEntity<ApiResponse<Object>> createBorrower(
            @Parameter(description = "Borrower registration details")
            @Valid @RequestBody RegisterBorrowerRequest userToRegister,
            @Parameter(description = "OTP received via email")
            @RequestParam String otp) {

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
    @Operation(summary = "Submit lender onboarding application",
            description = "Submits a lender application with required documents for system admin review")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Application submitted successfully",
                    content = @Content(schema = @Schema(implementation = LenderOnboardingResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Email already exists in system"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Application submission failed"
            )
    })
    public ResponseEntity<ApiResponse<Object>> lenderApplication(
            @Parameter(description = "Lender onboarding form with documents")
            @Valid @ModelAttribute LenderOnboardingForm form) {

        try {
            LenderOnboardingResponse lenderOnboardingApplication = lenderProfileService.createLenderOnboardingApplication(form);
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
