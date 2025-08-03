package com.loantrackr.controller;

import com.loantrackr.dto.request.LenderUpdateRequest;
import com.loantrackr.dto.response.*;
import com.loantrackr.enums.LoanStatus;
import com.loantrackr.exception.UserNotFoundException;
import com.loantrackr.service.LenderProfileService;
import com.loantrackr.service.LoanService;
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
public class LenderController {

    private final LenderProfileService lenderProfileService;
    private final LoanService loanService;

    @GetMapping("/info")
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
    public ResponseEntity<ApiResponse<Object>> updateLenderProfile(
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
    public ResponseEntity<LoanApplicationResponse> approveLoan(@PathVariable Long applicationId) {
        log.info("REST: Loan approval request - Application ID: {}", applicationId);
        LoanApplicationResponse response = loanService.approveLoan(applicationId);
        return ResponseEntity.ok(response);
    }


    @PutMapping("/applications/{applicationId}/reject")
    public ResponseEntity<LoanApplicationResponse> rejectLoan(@PathVariable Long applicationId) {
        log.info("REST: Loan rejection request - Application ID: {}", applicationId);
        LoanApplicationResponse response = loanService.rejectLoan(applicationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/applications/{applicationId}/disburse")
    public ResponseEntity<LoanDisbursementResponse> disburseLoan(@PathVariable Long applicationId) {
        log.info("REST: Loan disbursement request - Application ID: {}", applicationId);
        LoanDisbursementResponse response = loanService.disburseLoan(applicationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/applications")
    public ResponseEntity<List<LoanApplicationResponseForLender>> getMyLoanRequests() {
        log.info("REST: Fetching all loan requests for current lender");

        List<LoanApplicationResponseForLender> applications = loanService.getMyLoanRequests();

        log.info("REST: Successfully retrieved {} loan requests", applications.size());
        return ResponseEntity.ok(applications);
    }


    @GetMapping("/applications/status/{status}")
    public ResponseEntity<List<LoanApplicationResponseForLender>> getLoanRequestsByStatus(
            @PathVariable LoanStatus status) {
        log.info("REST: Fetching loan requests with status: {}", status);

        List<LoanApplicationResponseForLender> applications = loanService.getLoanRequestsByStatus(status);

        log.info("REST: Successfully retrieved {} loan requests with status: {}", applications.size(), status);
        return ResponseEntity.ok(applications);
    }


    @GetMapping("/applications/pending")
    public ResponseEntity<List<LoanApplicationResponseForLender>> getPendingLoans() {
        log.info("REST: Fetching pending loan applications");

        List<LoanApplicationResponseForLender> applications = loanService.getPendingLoans();

        log.info("REST: Successfully retrieved {} pending applications", applications.size());
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/applications/approved")
    public ResponseEntity<List<LoanApplicationResponseForLender>> getApprovedLoans() {
        log.info("REST: Fetching approved loan applications");

        List<LoanApplicationResponseForLender> applications = loanService.getApprovedLoans();

        log.info("REST: Successfully retrieved {} approved applications", applications.size());
        return ResponseEntity.ok(applications);
    }


    @GetMapping("/applications/rejected")
    public ResponseEntity<List<LoanApplicationResponseForLender>> getRejectedLoans() {
        log.info("REST: Fetching rejected loan applications");

        List<LoanApplicationResponseForLender> applications = loanService.getRejectedLoans();

        log.info("REST: Successfully retrieved {} rejected applications", applications.size());
        return ResponseEntity.ok(applications);
    }


    @GetMapping("/applications/disbursed")
    public ResponseEntity<List<LoanApplicationResponseForLender>> getDisbursedLoans() {
        log.info("REST: Fetching disbursed loan applications");

        List<LoanApplicationResponseForLender> applications = loanService.getDisbursedLoans();

        log.info("REST: Successfully retrieved {} disbursed applications", applications.size());
        return ResponseEntity.ok(applications);
    }


    @GetMapping("/loan/active")
    public ResponseEntity<List<LoanDetailsResponse>> getActiveLoanDetails() {
        log.info("REST: Fetching active loan details");

        List<LoanDetailsResponse> loans = loanService.getCurrentLenderActiveLoans();

        log.info("REST: Successfully retrieved {} active loans", loans.size());
        return ResponseEntity.ok(loans);
    }


    @GetMapping("/loan/completed")
    public ResponseEntity<List<LoanDetailsResponse>> getCompletedLoans() {
        log.info("REST: Fetching completed loan details");

        List<LoanDetailsResponse> loans = loanService.getCompletedLoans();

        log.info("REST: Successfully retrieved {} completed loans", loans.size());
        return ResponseEntity.ok(loans);
    }


    @GetMapping("/loan/{loanId}")
    public ResponseEntity<LoanDetailsResponse> getLoanById(@PathVariable Long loanId) {
        log.info("REST: Fetching loan details for loan ID: {}", loanId);

        LoanDetailsResponse loan = loanService.getLoanById(loanId);

        log.info("REST: Successfully retrieved loan details for loan ID: {}", loanId);
        return ResponseEntity.ok(loan);
    }


}
