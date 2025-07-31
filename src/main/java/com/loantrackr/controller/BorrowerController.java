package com.loantrackr.controller;

import com.loantrackr.dto.request.KycUpdateRequest;
import com.loantrackr.dto.request.UpdateUserRequest;
import com.loantrackr.dto.response.ApiResponse;
import com.loantrackr.dto.response.UserResponse;
import com.loantrackr.exception.OperationNotAllowedException;
import com.loantrackr.exception.UserNotFoundException;
import com.loantrackr.model.BorrowerKycDetails;
import com.loantrackr.service.BorrowerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/borrower")
@RequiredArgsConstructor
@Slf4j
public class BorrowerController {

    private final BorrowerService borrowerService;


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

}