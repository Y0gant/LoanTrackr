package com.loantrackr.controller;

import com.loantrackr.dto.request.RegisterUser;
import com.loantrackr.dto.response.ApiResponse;
import com.loantrackr.dto.response.LenderProfileResponse;
import com.loantrackr.dto.response.UserResponse;
import com.loantrackr.enums.Role;
import com.loantrackr.exception.InvalidRoleException;
import com.loantrackr.exception.OperationNotAllowedException;
import com.loantrackr.exception.UnauthorizedException;
import com.loantrackr.exception.UserNotFoundException;
import com.loantrackr.model.LenderOnboarding;
import com.loantrackr.model.User;
import com.loantrackr.service.SystemAdminService;
import com.loantrackr.service.UserService;
import com.loantrackr.util.SecurityUtils;
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
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/system-admin")
@Tag(name = "System Admin Management", description = "APIs for system administrator operations")
@SecurityRequirement(name = "bearerAuth")
public class SystemAdminController {

    private final SystemAdminService adminService;
    private final UserService userService;
    private final SystemAdminService systemAdminService;

    // System Admin Management Endpoints

    @GetMapping("/all")
    @Operation(summary = "Get all system administrators",
            description = "Retrieves a list of all system administrators in the system")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved system administrators",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<ApiResponse<Object>> getAllSystemAdmins() {
        log.info("Request received to fetch all system administrators");

        try {
            List<User> allSystemAdmins = adminService.getAllSystemAdmins();
            log.info("Successfully retrieved {} system administrators", allSystemAdmins.size());

            return ResponseEntity.ok(
                    ApiResponse.success(allSystemAdmins,
                            "Successfully retrieved " + allSystemAdmins.size() + " system administrators")
            );
        } catch (Exception e) {
            log.error("Error occurred while fetching system administrators", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve system administrators: " + e.getMessage()));
        }
    }

    @PostMapping("/new")
    @Operation(summary = "Create new system administrator",
            description = "Creates a new system administrator account (requires existing admin privileges)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "System administrator created successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or user not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Unauthorized - insufficient privileges"
            )
    })
    public ResponseEntity<ApiResponse<Object>> createNewSystemAdmin(
            @Valid @RequestBody RegisterUser registerUser) {

        log.info("Request received to create new system administrator with email: {}",
                registerUser.getEmail());

        try {
            User currentUser = getCurrentUser();
            log.debug("Current user ID: {} attempting to create system admin", currentUser.getId());

            UserResponse systemAdmin = adminService.createSystemAdmin(currentUser, registerUser);

            log.info("Successfully created system administrator with ID: {}", systemAdmin.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(systemAdmin, "System administrator created successfully"));

        } catch (UserNotFoundException e) {
            log.error("User not found while creating system admin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("User not found: " + e.getMessage()));

        } catch (UnauthorizedException e) {
            log.warn("Unauthorized attempt to create system admin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Unauthorized: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error while creating system administrator", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create system administrator: " + e.getMessage()));
        }
    }

    @PatchMapping("/verify")
    @Operation(summary = "Verify system administrator",
            description = "Marks a system administrator as verified")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "System administrator verified successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or user not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Unauthorized - insufficient privileges"
            )
    })
    public ResponseEntity<ApiResponse<Object>> verifySystemAdmin(
            @Parameter(description = "System administrator ID to verify")
            @RequestBody Long id) {

        log.info("Request received to verify system administrator with ID: {}", id);

        try {
            User currentUser = getCurrentUser();
            log.debug("Current user ID: {} attempting to verify system admin ID: {}",
                    currentUser.getId(), id);

            boolean verified = adminService.verifySystemAdmin(currentUser, id);

            log.info("Successfully verified system administrator with ID: {}", id);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success(verified, "System administrator verified successfully"));

        } catch (UserNotFoundException e) {
            log.error("User not found while verifying system admin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("User not found: " + e.getMessage()));

        } catch (UnauthorizedException e) {
            log.warn("Unauthorized attempt to verify system admin ID: {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Unauthorized: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error while verifying system administrator with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to verify system administrator: " + e.getMessage()));
        }
    }

    @DeleteMapping("/id")
    @Operation(summary = "Delete system administrator",
            description = "Soft deletes a system administrator account")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "System administrator deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or user not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Unauthorized or operation not allowed"
            )
    })
    public ResponseEntity<ApiResponse<Object>> deleteSystemAdmin(
            @Parameter(description = "System administrator ID to delete")
            @RequestBody long id) {

        log.info("Request received to delete system administrator with ID: {}", id);

        try {
            User currentUser = getCurrentUser();
            log.debug("Current user ID: {} attempting to delete system admin ID: {}",
                    currentUser.getId(), id);

            boolean deleted = adminService.deleteSystemAdmin(currentUser, id);

            log.warn("System administrator with ID: {} has been deleted by user ID: {}",
                    id, currentUser.getId());
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success(deleted, "System administrator deleted successfully"));

        } catch (UserNotFoundException e) {
            log.error("User not found while deleting system admin: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("User not found: " + e.getMessage()));

        } catch (UnauthorizedException e) {
            log.warn("Unauthorized attempt to delete system admin ID: {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Unauthorized: " + e.getMessage()));

        } catch (InvalidRoleException e) {
            log.error("Invalid role exception while deleting system admin ID: {}", id);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid role: " + e.getMessage()));

        } catch (OperationNotAllowedException e) {
            log.warn("Operation not allowed while deleting system admin ID: {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Operation not allowed: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error while deleting system administrator with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete system administrator: " + e.getMessage()));
        }
    }


    @PatchMapping("/lender/verify")
    @Operation(summary = "Verify lender",
            description = "Marks a lender as verified in the system")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "Lender verified successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or lender not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Unauthorized - only system admins can verify lenders"
            )
    })
    public ResponseEntity<ApiResponse<Object>> verifyLender(
            @Parameter(description = "Lender ID to verify")
            @RequestBody long id) {

        log.info("Request received to verify lender with ID: {}", id);

        try {
            User currentUser = getCurrentUser();
            validateSystemAdminRole(currentUser);

            log.debug("System admin ID: {} attempting to verify lender ID: {}",
                    currentUser.getId(), id);

            boolean verified = adminService.verifyLender(id);

            log.info("Successfully verified lender with ID: {}", id);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success(verified, "Lender verified successfully"));

        } catch (UserNotFoundException e) {
            log.error("Lender not found while verifying: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lender not found: " + e.getMessage()));

        } catch (UnauthorizedException e) {
            log.warn("Unauthorized attempt to verify lender ID: {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Unauthorized: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error while verifying lender with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to verify lender: " + e.getMessage()));
        }
    }

    @PostMapping("/lender/approve")
    @Operation(summary = "Approve lender request",
            description = "Approves a lender onboarding request and creates account")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Lender request approved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or lender request not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Unauthorized - only system admins can approve requests"
            )
    })
    public ResponseEntity<ApiResponse<Object>> approveLenderRequest(
            @Parameter(description = "Lender request ID to approve")
            @RequestBody long id) {

        log.info("Request received to approve lender request with ID: {}", id);

        try {
            User currentUser = getCurrentUser();
            validateSystemAdminRole(currentUser);

            log.debug("System admin ID: {} attempting to approve lender request ID: {}",
                    currentUser.getId(), id);

            boolean approved = adminService.approveLenderRequest(id);

            log.info("Successfully approved lender request with ID: {}", id);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(approved, "Lender request approved. Credentials will be emailed"));

        } catch (UserNotFoundException e) {
            log.error("Lender request not found while approving: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lender request not found: " + e.getMessage()));

        } catch (UnauthorizedException e) {
            log.warn("Unauthorized attempt to approve lender request ID: {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Unauthorized: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error while approving lender request with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to approve lender request: " + e.getMessage()));
        }
    }

    @PostMapping("/lender/reject")
    @Operation(summary = "Reject lender request",
            description = "Rejects a lender onboarding request with reason")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Lender request rejected successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or lender request not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Unauthorized - only system admins can reject requests"
            )
    })
    public ResponseEntity<ApiResponse<Object>> rejectLenderRequest(
            @Parameter(description = "Lender request ID to reject")
            @RequestParam long id,
            @Parameter(description = "Reason for rejection")
            @RequestBody String rejectionReason) {

        log.info("Request received to reject lender request with ID: {} for reason: {}",
                id, rejectionReason);

        try {
            User currentUser = getCurrentUser();
            validateSystemAdminRole(currentUser);

            log.debug("System admin ID: {} attempting to reject lender request ID: {}",
                    currentUser.getId(), id);

            boolean rejected = adminService.rejectLenderRequest(id, rejectionReason);

            log.info("Successfully rejected lender request with ID: {}", id);
            return ResponseEntity.ok()
                    .body(ApiResponse.success(rejected, "Lender request rejected successfully"));

        } catch (UserNotFoundException e) {
            log.error("Lender request not found while rejecting: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lender request not found: " + e.getMessage()));

        } catch (UnauthorizedException e) {
            log.warn("Unauthorized attempt to reject lender request ID: {}", id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Unauthorized: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error while rejecting lender request with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to reject lender request: " + e.getMessage()));
        }
    }

    @GetMapping("/lender/reviewed")
    @Operation(summary = "Get reviewed lender requests",
            description = "Retrieves all lender requests that have been reviewed")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved reviewed requests"
            )
    })
    public ResponseEntity<ApiResponse<Object>> getAllReviewedLenderRequests() {
        log.info("Request received to fetch all reviewed lender requests");

        try {
            var reviewedRequests = adminService.getAllReviewedRequests();
            log.info("Successfully retrieved {} reviewed lender requests", reviewedRequests.size());

            return ResponseEntity.ok()
                    .body(ApiResponse.success(reviewedRequests, "Successfully retrieved reviewed requests"));

        } catch (Exception e) {
            log.error("Error occurred while fetching reviewed lender requests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve reviewed requests: " + e.getMessage()));
        }
    }

    @GetMapping("/lender/pending")
    @Operation(summary = "Get pending lender requests",
            description = "Retrieves all pending lender onboarding requests")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved pending requests"
            )
    })
    public ResponseEntity<ApiResponse<Object>> getPendingRequests() {
        log.info("Request received to fetch all pending lender requests");

        try {
            var pendingRequests = adminService.getAllPendingLenderRequests();
            log.info("Successfully retrieved {} pending lender requests", pendingRequests.size());

            return ResponseEntity.ok()
                    .body(ApiResponse.success(pendingRequests, "Successfully retrieved pending lender requests"));

        } catch (Exception e) {
            log.error("Error occurred while fetching pending lender requests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve pending requests: " + e.getMessage()));
        }
    }

    @DeleteMapping("/lender/deactivate")
    @Operation(summary = "Deactivate lender",
            description = "Deactivates a lender account (soft delete)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "Lender deactivated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or lender not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Operation not allowed - lender has active loans"
            )
    })
    public ResponseEntity<ApiResponse<Object>> deactivateLender(
            @Parameter(description = "Lender ID to deactivate")
            @RequestParam long id) {

        log.info("Request received to deactivate lender with ID: {}", id);

        try {
            boolean deactivated = adminService.deactivateLender(id);

            log.warn("Lender with ID: {} has been deactivated", id);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success(deactivated, "Lender deactivated successfully"));

        } catch (OperationNotAllowedException e) {
            log.warn("Operation not allowed while deactivating lender ID: {} - {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Operation not allowed: " + e.getMessage()));

        } catch (UserNotFoundException e) {
            log.error("Lender not found while deactivating: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lender not found: " + e.getMessage()));

        } catch (IllegalStateException e) {
            log.error("Illegal state while deactivating lender ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(ApiResponse.error("System error: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error while deactivating lender with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to deactivate lender: " + e.getMessage()));
        }
    }

    @GetMapping("/lender")
    public ResponseEntity<ApiResponse<Object>> getLenderRequestById(@RequestParam long id) {
        try {
            LenderOnboarding lenderRequestDetails = systemAdminService.getLenderRequestDetails(id);
            if (lenderRequestDetails != null) {
                return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(lenderRequestDetails, "successfully retrieved lender request by id"));
            }
        } catch (OperationNotAllowedException e) {
            log.warn("Operation not allowed: - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Operation not allowed: " + e.getMessage()));

        } catch (UserNotFoundException e) {
            log.error("Lender request not found  {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lender request not found: " + e.getMessage()));

        } catch (IllegalStateException e) {
            log.error("Illegal state while fetching lender request {}", id, e);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(ApiResponse.error("System error: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error while fetching lender request with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch lender request: " + e.getMessage()));
        }
        return null;
    }

    @GetMapping("/lender/{id}/document/gst-certificate")
    public ResponseEntity<ApiResponse<Object>> getLenderGstCertificate(@PathVariable long id) {
        try {
            Resource gstCertificate = systemAdminService.getLenderDocument(id, "gst_certificate");
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(gstCertificate, "Successfully retrieved gst certificate."));
        } catch (OperationNotAllowedException e) {
            log.warn("Operation not allowed:  {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Operation not allowed: " + e.getMessage()));

        } catch (UserNotFoundException e) {
            log.error("Lender request not found  {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lender request not found: " + e.getMessage()));

        } catch (IllegalStateException e) {
            log.error("Illegal state while fetching lender request document {}", id, e);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(ApiResponse.error("System error: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error while fetching lender document with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch lender request: " + e.getMessage()));
        }

    }

    @GetMapping("/lender/{id}/document/rbi-license")
    public ResponseEntity<ApiResponse<Object>> getLenderRbiLicense(@PathVariable long id) {
        try {
            Resource rbiLicense = systemAdminService.getLenderDocument(id, "rbi_license");
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(rbiLicense, "Successfully retrieved rbi license."));
        } catch (OperationNotAllowedException e) {
            log.warn("Operation not allowed:  {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Operation not allowed: " + e.getMessage()));

        } catch (UserNotFoundException e) {
            log.error("Lender request not found  {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lender request not found: " + e.getMessage()));

        } catch (IllegalStateException e) {
            log.error("Illegal state while fetching lender request document {}", id, e);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(ApiResponse.error("System error: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error while fetching lender document with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch lender request: " + e.getMessage()));
        }

    }

    @GetMapping("/lender/{id}/document/pan")
    public ResponseEntity<ApiResponse<Object>> getLenderPanCard(@PathVariable long id) {
        try {
            Resource panCard = systemAdminService.getLenderDocument(id, "pan_card");
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(panCard, "Successfully retrieved pan card."));
        } catch (OperationNotAllowedException e) {
            log.warn("Operation not allowed:  {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Operation not allowed: " + e.getMessage()));

        } catch (UserNotFoundException e) {
            log.error("Lender request not found  {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lender request not found: " + e.getMessage()));

        } catch (IllegalStateException e) {
            log.error("Illegal state while fetching lender request document {}", id, e);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(ApiResponse.error("System error: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error while fetching lender document with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to fetch lender request: " + e.getMessage()));
        }

    }

    @GetMapping("/lenders")
    @Operation(summary = "Get all lenders",
            description = "Retrieves a list of all lenders in the system")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved lenders"
            )
    })
    public ResponseEntity<ApiResponse<Object>> getAllLenders() {
        log.info("Request received to fetch all lenders");

        try {
            List<LenderProfileResponse> allLenders = adminService.getAllLenders();
            log.info("Successfully retrieved {} lenders", allLenders.size());

            return ResponseEntity.ok()
                    .body(ApiResponse.success(allLenders, "Successfully retrieved " + allLenders.size() + " lenders"));

        } catch (Exception e) {
            log.error("Error occurred while fetching lenders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve lenders: " + e.getMessage()));
        }
    }

    @DeleteMapping("/lender")
    @Operation(summary = "Delete lender permanently",
            description = "Permanently deletes a lender account from the system")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "Lender deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or lender not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Operation not allowed - lender has active loans"
            )
    })
    public ResponseEntity<ApiResponse<Object>> deleteLender(
            @Parameter(description = "Lender ID to delete permanently")
            @RequestParam long id) {

        log.info("Request received to permanently delete lender with ID: {}", id);

        try {
            boolean deleted = adminService.permanentlyDeleteLender(id);

            log.warn("Lender with ID: {} has been permanently deleted", id);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success(deleted, "Lender deleted permanently"));

        } catch (OperationNotAllowedException e) {
            log.warn("Operation not allowed while deleting lender ID: {} - {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Operation not allowed: " + e.getMessage()));

        } catch (UserNotFoundException e) {
            log.error("Lender not found while deleting: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Lender not found: " + e.getMessage()));

        } catch (IllegalStateException e) {
            log.error("Illegal state while deleting lender ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body(ApiResponse.error("System error: " + e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error while deleting lender with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete lender: " + e.getMessage()));
        }
    }

    private User getCurrentUser() {
        String userName = SecurityUtils.getCurrentUserName();
        log.debug("Retrieving current user with username: {}", userName);

        Optional<User> userByUserName = userService.getUserByUserName(userName);
        if (userByUserName.isEmpty()) {
            log.error("Current user not found with username: {}", userName);
            throw new UserNotFoundException("Current user not found");
        }

        User user = userByUserName.get();
        log.debug("Current user retrieved: ID={}, Role={}", user.getId(), user.getRole());
        return user;
    }

    private void validateSystemAdminRole(User user) {
        if (!user.getRole().equals(Role.SYSTEM_ADMIN)) {
            log.warn("Access denied: User ID {} with role {} attempted system admin operation",
                    user.getId(), user.getRole());
            throw new UnauthorizedException("Only System admins can perform this operation");
        }
    }
}