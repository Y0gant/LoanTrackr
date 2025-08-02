package com.loantrackr.service;

import com.loantrackr.dto.request.RegisterUser;
import com.loantrackr.dto.response.LenderOnboardingResponse;
import com.loantrackr.dto.response.LenderProfileResponse;
import com.loantrackr.dto.response.UserResponse;
import com.loantrackr.enums.LoanStatus;
import com.loantrackr.enums.RequestStatus;
import com.loantrackr.enums.Role;
import com.loantrackr.exception.*;
import com.loantrackr.model.LenderOnboarding;
import com.loantrackr.model.LenderProfile;
import com.loantrackr.model.LoanApplication;
import com.loantrackr.model.User;
import com.loantrackr.repository.LenderOnboardingRepository;
import com.loantrackr.repository.LenderProfileRepository;
import com.loantrackr.repository.LoanApplicationRepository;
import com.loantrackr.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class SystemAdminService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final LenderProfileRepository lenderProfileRepository;
    private final LenderOnboardingRepository onboardingRepository;
    private final EmailService emailService;
    private final LenderProfileService lenderProfileService;
    private final OtpService otpService;
    private final ModelMapper modelMapper;
    private final LoanApplicationRepository loanApplicationRepository;
    private final FileStorageService fileStorageService;
    public String systemEmail;

    public SystemAdminService(UserService userService, UserRepository userRepository, LenderProfileRepository lenderProfileRepository, LenderOnboardingRepository onboardingRepository, EmailService emailService, LenderProfileService lenderProfileService, OtpService otpService, @Value("${bootstrap.email}") String firstEmail, ModelMapper modelMapper, LoanApplicationRepository loanApplicationRepository, FileStorageService fileStorageService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.lenderProfileRepository = lenderProfileRepository;
        this.onboardingRepository = onboardingRepository;
        this.emailService = emailService;
        this.lenderProfileService = lenderProfileService;
        this.otpService = otpService;
        this.systemEmail = firstEmail;
        this.modelMapper = modelMapper;
        this.loanApplicationRepository = loanApplicationRepository;
        this.fileStorageService = fileStorageService;
    }

    public static UserResponse toUserResponse(User user) {
        if (user == null) return null;

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .provider(user.getProvider())
                .isActive(user.isActive())
                .isEmailVerified(user.isEmailVerified())
                .build();
    }

    public boolean generateAndSendBootstrapOtp() {
        if (systemAdminExists()) {
            throw new SetupLockedException("A system admin already exists!");
        }
        return otpService.generateAndSendOtp(systemEmail);
    }

    public boolean systemAdminExists() {
        return userRepository.existsByRole(Role.SYSTEM_ADMIN);
    }

    @Transactional
    public UserResponse createInitialSystemAdmin(RegisterUser request, String otp) {
        log.info("Attempting to create initial SystemAdmin: {}", request.getEmail());

        otpService.validateOtp(systemEmail, otp);

        if (userService.existsByRole(Role.SYSTEM_ADMIN)) {
            log.error("Initial SystemAdmin creation blocked - a SYSTEM_ADMIN already exists.");
            throw new SetupLockedException("Initial SystemAdmin already exists.");
        }

        User admin = userService.createPrivilegedUser(request, Role.SYSTEM_ADMIN);
        admin.setEmailVerified(true);
        admin.setVerified(true);
        User saved = userRepository.save(admin);

        log.info("Initial SystemAdmin created and verified. ID: {}", saved.getId());
        return toUserResponse(saved);
    }

    @Transactional
    public UserResponse createSystemAdmin(User requester, RegisterUser request) {
        validateVerifiedAdmin(requester);

        log.info("Verified admin [{}] attempting to create new SystemAdmin: {}", requester.getId(), request.getEmail());

        User newAdmin = userService.createPrivilegedUser(request, Role.SYSTEM_ADMIN);
        newAdmin.setVerified(false);
        newAdmin.setEmailVerified(true);
        User saved = userRepository.save(newAdmin);
        UserResponse response = toUserResponse(saved);

        log.info("New unverified SystemAdmin created. ID: {}, Created by: {}", saved.getId(), requester.getId());
        return response;
    }

    @Transactional
    public boolean verifySystemAdmin(User requester, Long targetId) {
        validateVerifiedAdmin(requester);
        log.info("Verified admin [{}] attempting to verify SystemAdmin ID: {}", requester.getId(), targetId);
        boolean verified = userService.markUserVerified(targetId);
        log.info("SystemAdmin successfully verified. ID: {}, Verified by: {}", targetId, requester.getId());
        return verified;
    }

    @Transactional
    public boolean deleteSystemAdmin(User requester, Long targetId) {
        validateVerifiedAdmin(requester);

        if (requester.getId().equals(targetId)) {
            log.warn("Self-deletion blocked. Admin ID: {}", requester.getId());
            throw new OperationNotAllowedException("SystemAdmin cannot delete self.");
        }

        log.info("SystemAdmin [{}] attempting to delete SystemAdmin [{}]", requester.getId(), targetId);

        User target = userRepository.findUserById(targetId)
                .orElseThrow(() -> {
                    log.error("Delete failed. Target SystemAdmin not found. ID: {}", targetId);
                    return new UserNotFoundException("Target SystemAdmin not found.");
                });

        if (!target.getRole().equals(Role.SYSTEM_ADMIN)) {
            log.warn("Delete failed. Target user is not a SystemAdmin. ID: {}", targetId);
            throw new InvalidRoleException("Target is not a SystemAdmin.");
        }

        boolean deleted = userService.deleteUser(targetId);
        log.warn("SystemAdmin soft-deleted. ID: {}, Deleted by: {}", targetId, requester.getId());
        return deleted;
    }

    public List<User> getAllSystemAdmins() {
        log.debug("Fetching all users with SYSTEM_ADMIN role");
        return userRepository.findAllByRole(Role.SYSTEM_ADMIN);
    }

    private void validateVerifiedAdmin(User user) {
        if (user == null) {
            log.error("Access denied: Requester is null");
            throw new UnauthorizedException("Requester is null.");
        }

        if (!user.getRole().equals(Role.SYSTEM_ADMIN)) {
            log.error("Access denied: User ID {} is not a SystemAdmin", user.getId());
            throw new UnauthorizedException("Only SystemAdmins are allowed to perform this action.");
        }

        if (!user.isVerified()) {
            log.warn("Access denied: SystemAdmin ID {} is not verified", user.getId());
            throw new UnauthorizedException("SystemAdmin is not verified.");
        }
    }

    //    Lender Verification And Management
    @Transactional
    public boolean verifyLender(Long lenderId) {
        Optional<LenderProfile> lenderProfileOptional = lenderProfileRepository.findById(lenderId);
        if (lenderProfileOptional.isEmpty()) {
            throw new UserNotFoundException("No lender found for id :" + lenderId);
        }
        LenderProfile lenderProfile = lenderProfileOptional.get();
        if (lenderProfile.isVerified()) {
            return true;
        }
        boolean verified = userService.markUserVerified(lenderId);
        if (!verified) {
            throw new UserNotFoundException("User not found for lender with id :" + lenderId);
        }
        lenderProfile.setVerified(true);
        lenderProfileRepository.save(lenderProfile);
        return true;
    }

    public List<LenderOnboarding> getAllPendingLenderRequests() {
        return onboardingRepository.findAllByStatus(RequestStatus.PENDING);
    }

    public LenderOnboarding getLenderRequestDetails(Long requestId) {
        return onboardingRepository.findById(requestId)
                .orElseThrow(() -> new UserNotFoundException("Request not found with ID: " + requestId));
    }

    @Transactional
    public boolean rejectLenderRequest(Long requestId, String reason) {
        LenderOnboarding request = getLenderRequestDetails(requestId);
        request.setStatus(RequestStatus.REJECTED);
        request.setReviewed(true);
        request.setRejectionReason(reason);
        request.setProcessedAt(LocalDateTime.now());
        onboardingRepository.save(request);
        emailService.sendLenderRejectionEmail(request.getEmail(), request.getUsername(), reason);
        log.warn("Rejected lender onboarding request. ID: {}", requestId);
        return true;
    }

    @Transactional
    public boolean approveLenderRequest(Long requestId) {
        LenderOnboarding request = getLenderRequestDetails(requestId);

        if (userService.existsByEmail(request.getEmail())) {
            throw new UnauthorizedException("Email already exists. Cannot approve.");
        }

        String randomPassword = UUID.randomUUID().toString();

        RegisterUser reg = RegisterUser.builder()
                .email(request.getEmail())
                .username(UUID.randomUUID().toString())
                .password(randomPassword)
                .role(Role.LENDER)
                .build();

        User lenderUser = userService.createPrivilegedUser(reg, Role.LENDER);
        lenderUser.setVerified(true);
        userRepository.save(lenderUser);
        LenderProfile profileFromRequest = lenderProfileService.createProfileFromRequest(request, lenderUser);
        if (profileFromRequest == null) {
            throw new IllegalStateException("Unable to create lender profile from request");
        }
        emailService.sendLenderCredentials(request.getEmail(), lenderUser.getUsername(), randomPassword);
        request.setStatus(RequestStatus.APPROVED);
        request.setReviewed(true);
        request.setProcessedAt(LocalDateTime.now());
        onboardingRepository.save(request);
        log.info("Lender approved and account created for ID: {}", lenderUser.getId());
        return true;
    }

    public List<LenderOnboarding> getAllReviewedRequests() {
        return onboardingRepository.findAllByReviewedTrue();
    }

    public List<LenderOnboarding> getRequestsByStatus(RequestStatus status) {
        return onboardingRepository.findAllByStatus(status);
    }

    @Transactional
    public boolean deactivateLender(Long lenderUserId) {
        LenderProfile lenderProfile = getLenderProfile(lenderUserId);
        List<LoanApplication> loanApplicationByLender = loanApplicationRepository.findLoanApplicationByLender(lenderProfile);
        boolean existsActiveLoans = loanApplicationByLender.stream()
                .anyMatch(x -> x.getStatus() == LoanStatus.APPROVED
                        || x.getStatus() == LoanStatus.PENDING
                        || x.getStatus() == LoanStatus.DISBURSED);
        if (existsActiveLoans) {
            throw new OperationNotAllowedException("Cannot deactivate lender as it has active loans");
        }
        lenderProfile.setVerified(false);
        lenderProfileRepository.save(lenderProfile);
        return userService.deactivateUser(lenderUserId);
    }

    @Transactional
    public boolean permanentlyDeleteLender(Long userId) {
        LenderProfile lenderProfile = getLenderProfile(userId);
        List<LoanApplication> loanApplicationByLender = loanApplicationRepository.findLoanApplicationByLender(lenderProfile);
        boolean existsActiveLoans = loanApplicationByLender.stream()
                .anyMatch(x -> x.getStatus() == LoanStatus.APPROVED
                        || x.getStatus() == LoanStatus.PENDING
                        || x.getStatus() == LoanStatus.DISBURSED);
        if (existsActiveLoans) {
            throw new OperationNotAllowedException("Cannot deactivate lender as it has active loans");
        }
        userService.deleteUser(userId);
        log.warn("Lender permanently deleted. ID: {}", userId);
        return true;
    }

    public List<LenderProfileResponse> getAllLenders() {
        return lenderProfileRepository.findAll().stream().map((element) -> modelMapper.map(element, LenderProfileResponse.class)).toList();
    }

    public LenderProfile getLenderProfile(Long userId) {
        return lenderProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Lender profile not found for user ID: " + userId));
    }

    public LenderOnboardingResponse mapToResponse(LenderOnboarding onboarding) {
        if (onboarding == null) return null;

        return LenderOnboardingResponse.builder()
                .id(onboarding.getId())
                .username(onboarding.getUsername())
                .email(onboarding.getEmail())
                .contactPersonName(onboarding.getContactPersonName())
                .organizationName(onboarding.getOrganizationName())
                .gstin(onboarding.getGstin())
                .rbiLicenseNumber(onboarding.getRbiLicenseNumber())
                .gstCertificate(onboarding.getGstCertificate() != null && !onboarding.getGstCertificate().isBlank())
                .panCard(onboarding.getPanCard() != null && !onboarding.getPanCard().isBlank())
                .rbiLicense(onboarding.getRbiLicense() != null && !onboarding.getRbiLicenseNumber().isBlank())
                .reviewed(onboarding.isReviewed())
                .status(onboarding.getStatus())
                .rejectionReason(onboarding.getRejectionReason())
                .requestedAt(onboarding.getRequestedAt())
                .processedAt(onboarding.getProcessedAt())
                .build();
    }


    public Resource getLenderDocument(long lenderId, String document) {
        LenderOnboarding lenderRequest = getLenderRequestDetails(lenderId);
        if (document.equals("gst_certificate") && lenderRequest.getGstCertificate() != null && !lenderRequest.getGstCertificate().isBlank()) {
            return fileStorageService.loadFile(lenderRequest.getGstCertificate());
        } else if (document.equals("pan_card") && lenderRequest.getPanCard() != null && !lenderRequest.getPanCard().isBlank()) {
            return fileStorageService.loadFile(lenderRequest.getPanCard());

        } else if (document.equals("rbi_license") && lenderRequest.getRbiLicense() != null && !lenderRequest.getRbiLicense().isBlank()) {
            return fileStorageService.loadFile(lenderRequest.getRbiLicense());
        } else {
            return null;
        }
    }
}
