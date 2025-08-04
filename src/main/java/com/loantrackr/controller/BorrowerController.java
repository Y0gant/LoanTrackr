package com.loantrackr.controller;

import com.loantrackr.dto.request.KycUpdateRequest;
import com.loantrackr.dto.request.LoanApplicationRequest;
import com.loantrackr.dto.request.PaymentRequest;
import com.loantrackr.dto.request.UpdateUserRequest;
import com.loantrackr.dto.response.*;
import com.loantrackr.exception.OperationNotAllowedException;
import com.loantrackr.exception.UserNotFoundException;
import com.loantrackr.model.BorrowerKycDetails;
import com.loantrackr.model.LoanPayment;
import com.loantrackr.model.LoanRepaymentSchedule;
import com.loantrackr.service.BorrowerService;
import com.loantrackr.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/v1/borrower")
@PreAuthorize("hasRole('BORROWER')")
@Validated
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Borrower Profile and Loan Operations",
        description = "APIs for borrower profile management, KYC verification, loan applications, and loan repayment")
@SecurityRequirement(name = "bearerAuth")
public class BorrowerController {

    private final BorrowerService borrowerService;
    private final LoanService loanService;


    @GetMapping("/info")
    @Operation(summary = "Get borrower profile information",
            description = "Retrieves the current borrower's profile details and account information")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Borrower profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "User not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<ApiResponse<Object>> getBorrowerInfo() {
        log.info("Received request to get borrower info");

        try {
            UserResponse borrowerInfo = borrowerService.getInfo();
            log.info("Successfully retrieved borrower info for user: {}", borrowerInfo.getUsername());
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(borrowerInfo, "Successfully retrieved user info"));

        } catch (UserNotFoundException e) {
            log.error("User not found while retrieving borrower info: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Error retrieving user info, user not found."));
        } catch (Exception e) {
            log.error("Unexpected error occurred while retrieving borrower info: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Unexpected error occurred while retrieving user info."));
        }
    }

    @GetMapping("/kyc")
    @Operation(summary = "Get KYC information",
            description = "Retrieves the current borrower's KYC verification details and status")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "KYC details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = BorrowerKycDetails.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "User not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Failed to retrieve KYC details"
            )
    })
    public ResponseEntity<ApiResponse<Object>> getKycInfo() {
        log.info("Received request to get KYC information");

        try {
            BorrowerKycDetails kycDetails = borrowerService.getKycInfo();
            log.info("Successfully retrieved KYC details for user ID: {}", kycDetails.getUser().getId());
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(kycDetails, "Successfully retrieved KYC details"));

        } catch (UserNotFoundException e) {
            log.error("User not found while retrieving KYC info: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("User not found: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error occurred while retrieving KYC info: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve KYC details for user: " + e.getMessage()));
        }
    }

    @PutMapping("/kyc")
    @Operation(summary = "Update KYC information",
            description = "Updates the borrower's KYC details including PAN, Aadhaar, and address information")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "KYC information updated successfully",
                    content = @Content(schema = @Schema(implementation = BorrowerKycDetails.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid update data or user not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Failed to update KYC details"
            )
    })
    public ResponseEntity<ApiResponse<Object>> updateKycInfo(
            @Parameter(description = "KYC update details")
            @Valid @RequestBody KycUpdateRequest request) {
        log.info("Received request to update KYC information");
        log.debug("KYC update request details: PAN changed: {}, Aadhaar changed: {}, Address changed: {}",
                request.getPanNumber() != null,
                request.getAadhaarNumber() != null,
                request.getAddress() != null);

        try {
            BorrowerKycDetails updatedKyc = borrowerService.updateKycInfo(request);
            log.info("Successfully updated KYC information for user ID: {}, KYC Status: {}",
                    updatedKyc.getUser().getId(),
                    updatedKyc.isKycVerified() ? "Verified" : "Pending Verification");

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(updatedKyc, "Successfully updated KYC information for user"));

        } catch (UserNotFoundException e) {
            log.error("User not found while updating KYC info: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("User not found: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error occurred while updating KYC info: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update KYC details for user: " + e.getMessage()));
        }
    }


    @PutMapping("/profile")
    @Operation(summary = "Update borrower profile",
            description = "Updates the borrower's profile information including username, email, and password")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "User profile updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid update data or user not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Failed to update profile"
            )
    })
    public ResponseEntity<ApiResponse<Object>> updateUser(
            @Parameter(description = "Profile update details")
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("Received request to update user profile");
        log.debug("User update request for fields: name={}, email={}, password={}",
                request.getUsername() != null,
                request.getEmail() != null,
                request.getPassword() != null);

        try {
            UserResponse updatedUser = borrowerService.updateUser(request);
            log.info("Successfully updated user profile for username: {}", updatedUser.getUsername());
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(updatedUser, "Successfully updated user"));

        } catch (UserNotFoundException e) {
            log.error("User not found while updating profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("User not found: " + e.getMessage()));


        } catch (Exception e) {
            log.error("Unexpected error occurred while updating user profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update details for user: " + e.getMessage()));
        }
    }


    @DeleteMapping("/account")
    @Operation(summary = "Delete borrower account",
            description = "Soft deletes the borrower's account and all associated data")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Borrower account deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "User not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Account deletion not allowed - active loans exist"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Failed to delete account"
            )
    })
    public ResponseEntity<ApiResponse<Object>> deleteBorrower() {
        log.info("Received request to delete borrower account");

        try {
            boolean deleted = borrowerService.deleteBorrower();

            if (deleted) {
                log.info("Successfully deleted borrower account");
                return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(true, "Successfully deleted user"));
            } else {
                log.warn("Failed to delete borrower account - operation returned false");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error("Failed to delete user: "));

            }

        } catch (UserNotFoundException e) {
            log.error("User not found while deleting account: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("User not found: " + e.getMessage()));

        } catch (OperationNotAllowedException e) {
            log.error("Account deletion not allowed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Operation not allowed: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error occurred while deleting borrower account: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete user: " + e.getMessage()));
        }
    }

    //Loan


    @GetMapping("/loan/lenders")
    @Operation(summary = "Get all active lenders",
            description = "Retrieves a list of all active lenders available for loan applications")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Active lenders retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LenderSummaryResponse.class))
            )
    })
    public ResponseEntity<List<LenderSummaryResponse>> getAllActiveLenders() {
        log.info("REST: Fetching all active lenders");
        List<LenderSummaryResponse> lenders = loanService.getAllActiveLenderResponses();
        return ResponseEntity.ok(lenders);
    }

    @GetMapping("/loan/lenders/{lenderId}/emi-preview")
    @Operation(summary = "Preview EMI calculation",
            description = "Calculates and previews EMI details for a loan with specific lender")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "EMI preview calculated successfully",
                    content = @Content(schema = @Schema(implementation = EmiPreview.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid principal amount or tenure"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Lender not found"
            )
    })
    public ResponseEntity<EmiPreview> previewEmi(
            @Parameter(description = "Lender ID to calculate EMI for")
            @PathVariable Long lenderId,
            @Parameter(description = "Principal loan amount (minimum 1000)")
            @RequestParam @DecimalMin(value = "1000.00", message = "Principal amount must be at least 1000") BigDecimal principal,
            @Parameter(description = "Loan tenure in months (minimum 6)")
            @RequestParam @Min(value = 6, message = "Tenure must be at least 6 months") int tenure) {
        log.info("REST: EMI preview request - Lender: {}, Principal: {}, Tenure: {}", lenderId, principal, tenure);
        EmiPreview preview = loanService.previewEmiFor(lenderId, principal, tenure);
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/loan/apply/{lenderId}")
    @Operation(summary = "Apply for loan",
            description = "Submits a loan application to a specific lender")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Loan application submitted successfully",
                    content = @Content(schema = @Schema(implementation = LoanApplicationResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid loan application data or KYC incomplete"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Lender not found"
            )
    })
    public ResponseEntity<LoanApplicationResponse> applyForLoan(
            @Parameter(description = "Lender ID to apply loan with")
            @PathVariable Long lenderId,
            @Parameter(description = "Loan application details")
            @Valid @RequestBody LoanApplicationRequest request) {

        log.info("REST: Loan application request - Lender: {}, Amount: {}", lenderId, request.getLoanAmount());
        LoanApplicationResponse response = loanService.applyLoan(lenderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/loan/applications/withdraw")
    @Operation(summary = "Withdraw loan application",
            description = "Withdraws a pending loan application")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Loan application withdrawn successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "No pending loan application to withdraw"
            )
    })
    public ResponseEntity<String> withdrawLoanApplication() {
        log.info("REST: Loan withdrawal request");
        boolean withdrawn = loanService.withdrawLoan();

        if (withdrawn) {
            return ResponseEntity.ok("Loan application withdrawn successfully");
        } else {
            return ResponseEntity.badRequest().body("Failed to withdraw loan application");
        }
    }


    @GetMapping("/loan/applications/my")
    @Operation(summary = "Get my loan applications",
            description = "Retrieves all loan applications submitted by the current borrower")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Loan applications retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LoanApplicationResponse.class))
            )
    })
    public ResponseEntity<List<LoanApplicationResponse>> getMyLoanApplications() {
        log.info("REST: Fetching user's loan applications");
        List<LoanApplicationResponse> applications = loanService.getMyLoanApplications();
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/loan/{loanId}/payments/history")
    @Operation(summary = "Get loan payment history",
            description = "Retrieves complete payment history for a specific loan")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Payment history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LoanPayment.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Loan not found or access denied"
            )
    })
    public ResponseEntity<List<LoanPayment>> getPaymentHistory(
            @Parameter(description = "Loan ID to get payment history for")
            @PathVariable Long loanId) {
        UserResponse info = borrowerService.getInfo();
        log.info("REST: Payment history request - Loan ID: {}", loanId);
        LoanDetailsResponse loanById = loanService.getLoanById(loanId);
        if (!loanById.getBorrowerName().equals(info.getUsername())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        List<LoanPayment> history = loanService.getPaymentHistory(loanId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/loan/{loanId}/schedule")
    @Operation(summary = "Get loan repayment schedule",
            description = "Retrieves the complete EMI repayment schedule for a specific loan")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Repayment schedule retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LoanRepaymentSchedule.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Loan not found"
            )
    })
    public ResponseEntity<List<LoanRepaymentSchedule>> getPaymentSchedule(
            @Parameter(description = "Loan ID to get repayment schedule for")
            @PathVariable Long loanId) {
        log.info("REST: Payment schedule request - Loan ID: {}", loanId);
        List<LoanRepaymentSchedule> schedule = loanService.getPaymentSchedule(loanId);
        return ResponseEntity.ok(schedule);
    }

    @PostMapping("/loan/{loanId}/payments")
    @Operation(summary = "Make loan payment",
            description = "Processes a payment for a specific loan (EMI or partial payment)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Payment processed successfully",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid payment amount or loan status"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Loan not found"
            )
    })
    public ResponseEntity<PaymentResponse> makePayment(
            @Parameter(description = "Loan ID to make payment for")
            @PathVariable Long loanId,
            @Parameter(description = "Payment details")
            @Valid @RequestBody PaymentRequest request) {

        log.info("REST: Payment request - Loan ID: {}, Amount: {}", loanId, request.getAmount());
        PaymentResponse response = loanService.makePayment(loanId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}