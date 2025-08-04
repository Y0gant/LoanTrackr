package com.loantrackr.controller;

import com.loantrackr.dto.request.LenderUpdateRequest;
import com.loantrackr.dto.response.*;
import com.loantrackr.enums.LoanStatus;
import com.loantrackr.exception.UserNotFoundException;
import com.loantrackr.service.LenderProfileService;
import com.loantrackr.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/lender")
@PreAuthorize("hasRole('LENDER')")
@Validated
@RequiredArgsConstructor
@Tag(name = "Lender Profile and Loan Management",
        description = "APIs for lender profile operations and loan application management")
@SecurityRequirement(name = "bearerAuth")
public class LenderController {

    private final LenderProfileService lenderProfileService;
    private final LoanService loanService;

    @GetMapping("/info")
    @Operation(summary = "Get lender profile information",
            description = "Retrieves the current lender's profile details and business information")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Lender profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LenderProfileResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Lender not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<ApiResponse<Object>> getLenderById() {

        log.info("Request received to fetch lender profile ");

        try {
            LenderProfileResponse lenderProfile = lenderProfileService.getCurrentLender();

            log.info("Successfully retrieved lender profile.");
            return ResponseEntity.ok(
                    ApiResponse.success(lenderProfile, "Lender profile retrieved successfully"));

        } catch (UserNotFoundException e) {
            log.error("Lender not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Lender not found: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error while retrieving lender profile ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve lender profile: " + e.getMessage()));
        }
    }

    @PatchMapping("/update")
    @Operation(summary = "Update lender profile",
            description = "Updates the current lender's profile information")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Lender profile updated successfully",
                    content = @Content(schema = @Schema(implementation = LenderSummaryResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Lender not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid update data"
            )
    })
    public ResponseEntity<ApiResponse<Object>> updateLenderProfile(
            @Parameter(description = "Lender profile update details")
            @Valid @RequestBody LenderUpdateRequest request) {


        log.info("Request received to update lender profile");

        try {
            LenderSummaryResponse updatedProfile = lenderProfileService.updateLenderProfile(request);

            log.info("Successfully updated lender profile for lender ID: {}",
                    updatedProfile.getLenderId());

            return ResponseEntity.ok(
                    ApiResponse.success(updatedProfile, "Lender profile updated successfully"));

        } catch (UserNotFoundException e) {
            log.error("User not found while updating lender profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("User not found: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error while updating lender profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update lender profile: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete lender profile",
            description = "Soft deletes the current lender's profile and account")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "Lender profile deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Lender not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<ApiResponse<Object>> deleteLender() {
        log.info("Request received to delete current lender profile");

        try {
            boolean deleted = lenderProfileService.deleteLender();

            log.warn("Lender profile has been successfully deleted");
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success(deleted, "Lender profile deleted successfully"));

        } catch (UserNotFoundException e) {
            log.error("Lender not found while deleting profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Lender not found: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error while deleting lender profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete lender profile: " + e.getMessage()));
        }
    }


    @PutMapping("/applications/{applicationId}/approve")
    @Operation(summary = "Approve loan application",
            description = "Approves a pending loan application and sets it ready for disbursement")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Loan application approved successfully",
                    content = @Content(schema = @Schema(implementation = LoanApplicationResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Loan application not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Loan application cannot be approved"
            )
    })
    public ResponseEntity<LoanApplicationResponse> approveLoan(
            @Parameter(description = "Loan application ID to approve")
            @PathVariable Long applicationId) {
        log.info("REST: Loan approval request - Application ID: {}", applicationId);
        LoanApplicationResponse response = loanService.approveLoan(applicationId);
        return ResponseEntity.ok(response);
    }


    @PutMapping("/applications/{applicationId}/reject")
    @Operation(summary = "Reject loan application",
            description = "Rejects a pending loan application with rejection reason")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Loan application rejected successfully",
                    content = @Content(schema = @Schema(implementation = LoanApplicationResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Loan application not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Loan application cannot be rejected"
            )
    })
    public ResponseEntity<LoanApplicationResponse> rejectLoan(
            @Parameter(description = "Loan application ID to reject")
            @PathVariable Long applicationId) {
        log.info("REST: Loan rejection request - Application ID: {}", applicationId);
        LoanApplicationResponse response = loanService.rejectLoan(applicationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/applications/{applicationId}/disburse")
    @Operation(summary = "Disburse approved loan",
            description = "Disburses funds for an approved loan application and activates the loan")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Loan disbursed successfully",
                    content = @Content(schema = @Schema(implementation = LoanDisbursementResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Loan application not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Loan not ready for disbursement"
            )
    })
    public ResponseEntity<LoanDisbursementResponse> disburseLoan(
            @Parameter(description = "Loan application ID to disburse")
            @PathVariable Long applicationId) {
        log.info("REST: Loan disbursement request - Application ID: {}", applicationId);
        LoanDisbursementResponse response = loanService.disburseLoan(applicationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/applications")
    @Operation(summary = "Get all loan requests",
            description = "Retrieves all loan applications submitted to the current lender")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Loan requests retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LoanApplicationResponseForLender.class))
            )
    })
    public ResponseEntity<List<LoanApplicationResponseForLender>> getMyLoanRequests() {
        log.info("REST: Fetching all loan requests for current lender");

        List<LoanApplicationResponseForLender> applications = loanService.getMyLoanRequests();

        log.info("REST: Successfully retrieved {} loan requests", applications.size());
        return ResponseEntity.ok(applications);
    }


    @GetMapping("/applications/status/{status}")
    @Operation(summary = "Get loan requests by status",
            description = "Retrieves loan applications filtered by specific status")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Loan requests retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LoanApplicationResponseForLender.class))
            )
    })
    public ResponseEntity<List<LoanApplicationResponseForLender>> getLoanRequestsByStatus(
            @Parameter(description = "Loan status to filter by")
            @PathVariable LoanStatus status) {
        log.info("REST: Fetching loan requests with status: {}", status);

        List<LoanApplicationResponseForLender> applications = loanService.getLoanRequestsByStatus(status);

        log.info("REST: Successfully retrieved {} loan requests with status: {}", applications.size(), status);
        return ResponseEntity.ok(applications);
    }


    @GetMapping("/applications/pending")
    @Operation(summary = "Get pending loan applications",
            description = "Retrieves all loan applications awaiting lender review")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Pending loan applications retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LoanApplicationResponseForLender.class))
            )
    })
    public ResponseEntity<List<LoanApplicationResponseForLender>> getPendingLoans() {
        log.info("REST: Fetching pending loan applications");

        List<LoanApplicationResponseForLender> applications = loanService.getPendingLoans();

        log.info("REST: Successfully retrieved {} pending applications", applications.size());
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/applications/approved")
    @Operation(summary = "Get approved loan applications",
            description = "Retrieves all loan applications that have been approved")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Approved loan applications retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LoanApplicationResponseForLender.class))
            )
    })
    public ResponseEntity<List<LoanApplicationResponseForLender>> getApprovedLoans() {
        log.info("REST: Fetching approved loan applications");

        List<LoanApplicationResponseForLender> applications = loanService.getApprovedLoans();

        log.info("REST: Successfully retrieved {} approved applications", applications.size());
        return ResponseEntity.ok(applications);
    }


    @GetMapping("/applications/rejected")
    @Operation(summary = "Get rejected loan applications",
            description = "Retrieves all loan applications that have been rejected")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Rejected loan applications retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LoanApplicationResponseForLender.class))
            )
    })
    public ResponseEntity<List<LoanApplicationResponseForLender>> getRejectedLoans() {
        log.info("REST: Fetching rejected loan applications");

        List<LoanApplicationResponseForLender> applications = loanService.getRejectedLoans();

        log.info("REST: Successfully retrieved {} rejected applications", applications.size());
        return ResponseEntity.ok(applications);
    }


    @GetMapping("/applications/disbursed")
    @Operation(summary = "Get disbursed loan applications",
            description = "Retrieves all loan applications that have been disbursed")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Disbursed loan applications retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LoanApplicationResponseForLender.class))
            )
    })
    public ResponseEntity<List<LoanApplicationResponseForLender>> getDisbursedLoans() {
        log.info("REST: Fetching disbursed loan applications");

        List<LoanApplicationResponseForLender> applications = loanService.getDisbursedLoans();

        log.info("REST: Successfully retrieved {} disbursed applications", applications.size());
        return ResponseEntity.ok(applications);
    }


    @GetMapping("/loan/active")
    @Operation(summary = "Get active loans",
            description = "Retrieves all currently active loans for the lender")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Active loans retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LoanDetailsResponse.class))
            )
    })
    public ResponseEntity<List<LoanDetailsResponse>> getActiveLoanDetails() {
        log.info("REST: Fetching active loan details");

        List<LoanDetailsResponse> loans = loanService.getCurrentLenderActiveLoans();

        log.info("REST: Successfully retrieved {} active loans", loans.size());
        return ResponseEntity.ok(loans);
    }


    @GetMapping("/loan/completed")
    @Operation(summary = "Get completed loans",
            description = "Retrieves all completed/fully repaid loans for the lender")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Completed loans retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LoanDetailsResponse.class))
            )
    })
    public ResponseEntity<List<LoanDetailsResponse>> getCompletedLoans() {
        log.info("REST: Fetching completed loan details");

        List<LoanDetailsResponse> loans = loanService.getCompletedLoans();

        log.info("REST: Successfully retrieved {} completed loans", loans.size());
        return ResponseEntity.ok(loans);
    }


    @GetMapping("/loan/{loanId}")
    @Operation(summary = "Get loan details by ID",
            description = "Retrieves detailed information for a specific loan")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Loan details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = LoanDetailsResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Loan not found"
            )
    })
    public ResponseEntity<LoanDetailsResponse> getLoanById(
            @Parameter(description = "Loan ID to retrieve details for")
            @PathVariable Long loanId) {
        log.info("REST: Fetching loan details for loan ID: {}", loanId);

        LoanDetailsResponse loan = loanService.getLoanById(loanId);

        log.info("REST: Successfully retrieved loan details for loan ID: {}", loanId);
        return ResponseEntity.ok(loan);
    }


}
