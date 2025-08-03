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
public class BorrowerController {

    private final BorrowerService borrowerService;
    private final LoanService loanService;


    @GetMapping("/info")
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
    public ResponseEntity<ApiResponse<Object>> updateKycInfo(@Valid @RequestBody KycUpdateRequest request) {
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
    public ResponseEntity<ApiResponse<Object>> updateUser(@Valid @RequestBody UpdateUserRequest request) {
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
    public ResponseEntity<List<LenderSummaryResponse>> getAllActiveLenders() {
        log.info("REST: Fetching all active lenders");
        List<LenderSummaryResponse> lenders = loanService.getAllActiveLenderResponses();
        return ResponseEntity.ok(lenders);
    }

    @GetMapping("/loan/lenders/{lenderId}/emi-preview")
    public ResponseEntity<EmiPreview> previewEmi(
            @PathVariable Long lenderId,
            @RequestParam @DecimalMin(value = "1000.00", message = "Principal amount must be at least 1000") BigDecimal principal,
            @RequestParam @Min(value = 6, message = "Tenure must be at least 6 months") int tenure) {

        log.info("REST: EMI preview request - Lender: {}, Principal: {}, Tenure: {}", lenderId, principal, tenure);
        EmiPreview preview = loanService.previewEmiFor(lenderId, principal, tenure);
        return ResponseEntity.ok(preview);
    }

    @PostMapping("/loan/apply/{lenderId}")
    public ResponseEntity<LoanApplicationResponse> applyForLoan(
            @PathVariable Long lenderId,
            @Valid @RequestBody LoanApplicationRequest request) {

        log.info("REST: Loan application request - Lender: {}, Amount: {}", lenderId, request.getLoanAmount());
        LoanApplicationResponse response = loanService.applyLoan(lenderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/loan/applications/withdraw")
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
    public ResponseEntity<List<LoanApplicationResponse>> getMyLoanApplications() {
        log.info("REST: Fetching user's loan applications");
        List<LoanApplicationResponse> applications = loanService.getMyLoanApplications();
        return ResponseEntity.ok(applications);
    }

    @GetMapping("/loan/{loanId}/payments/history")
    public ResponseEntity<List<LoanPayment>> getPaymentHistory(@PathVariable Long loanId) {
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
    public ResponseEntity<List<LoanRepaymentSchedule>> getPaymentSchedule(@PathVariable Long loanId) {
        log.info("REST: Payment schedule request - Loan ID: {}", loanId);
        List<LoanRepaymentSchedule> schedule = loanService.getPaymentSchedule(loanId);
        return ResponseEntity.ok(schedule);
    }

    @PostMapping("/loan/{loanId}/payments")
    public ResponseEntity<PaymentResponse> makePayment(
            @PathVariable Long loanId,
            @Valid @RequestBody PaymentRequest request) {

        log.info("REST: Payment request - Loan ID: {}, Amount: {}", loanId, request.getAmount());
        PaymentResponse response = loanService.makePayment(loanId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}