package com.loantrackr.controller;

import com.loantrackr.dto.request.LenderUpdateRequest;
import com.loantrackr.dto.response.ApiResponse;
import com.loantrackr.dto.response.LenderProfileResponse;
import com.loantrackr.dto.response.LenderSummaryResponse;
import com.loantrackr.exception.UserNotFoundException;
import com.loantrackr.service.LenderProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/lender")
@RequiredArgsConstructor
public class LenderController {

    private final LenderProfileService lenderProfileService;

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
}
