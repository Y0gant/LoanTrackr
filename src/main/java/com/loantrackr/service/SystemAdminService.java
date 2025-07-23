package com.loantrackr.service;

import com.loantrackr.dto.request.RegisterUser;
import com.loantrackr.enums.RequestStatus;
import com.loantrackr.enums.Role;
import com.loantrackr.exception.*;
import com.loantrackr.model.LenderOnboarding;
import com.loantrackr.model.LenderProfile;
import com.loantrackr.model.User;
import com.loantrackr.repository.LenderOnboardingRepository;
import com.loantrackr.repository.LenderProfileRepository;
import com.loantrackr.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    public String systemEmail;

    public SystemAdminService(UserService userService, UserRepository userRepository, LenderProfileRepository lenderProfileRepository, LenderOnboardingRepository onboardingRepository, EmailService emailService, LenderProfileService lenderProfileService, OtpService otpService, @Value("${bootstrap.email}") String firstEmail) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.lenderProfileRepository = lenderProfileRepository;
        this.onboardingRepository = onboardingRepository;
        this.emailService = emailService;
        this.lenderProfileService = lenderProfileService;
        this.otpService = otpService;
        this.systemEmail = firstEmail;
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
    public User createInitialSystemAdmin(RegisterUser request, String otp) {
        log.info("Attempting to create initial SystemAdmin: {}", request.getEmail());

        otpService.validateOtp(systemEmail, otp);

        if (userService.existsByRole(Role.SYSTEM_ADMIN)) {
            log.error("Initial SystemAdmin creation blocked - a SYSTEM_ADMIN already exists.");
            throw new SetupLockedException("Initial SystemAdmin already exists.");
        }

        User admin = userService.createPrivilegedUser(request, Role.SYSTEM_ADMIN);
        admin.setVerified(true);
        User saved = userRepository.save(admin);

        log.info("Initial SystemAdmin created and verified. ID: {}", saved.getId());
        return saved;
    }


    @Transactional
    public User createSystemAdmin(User requester, RegisterUser request) {
        validateVerifiedAdmin(requester);

        log.info("Verified admin [{}] attempting to create new SystemAdmin: {}", requester.getId(), request.getEmail());

        User newAdmin = userService.createPrivilegedUser(request, Role.SYSTEM_ADMIN);
        newAdmin.setVerified(false);
        User saved = userRepository.save(newAdmin);

        log.info("New unverified SystemAdmin created. ID: {}, Created by: {}", saved.getId(), requester.getId());
        return saved;
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
                .username(request.getUsername())
                .password(randomPassword)
                .role(Role.LENDER)
                .build();

        User lenderUser = userService.createPrivilegedUser(reg, Role.LENDER);
        lenderUser.setVerified(true);
        userRepository.save(lenderUser);
        LenderProfile profileFromRequest = lenderProfileService.createProfileFromRequest(request, lenderUser);
        emailService.sendLenderCredentials(request.getEmail(), lenderUser.getUsername(), randomPassword);
        onboardingRepository.delete(request);
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
    public boolean markLenderVerified(Long lenderUserId) {
        User user = userService.getUserByID(lenderUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.getRole().equals(Role.LENDER)) {
            throw new InvalidRoleException("Not a LENDER user.");
        }

        user.setVerified(true);
        userRepository.save(user);
        return true;
    }

    @Transactional
    public boolean deactivateLender(Long lenderUserId) {
        LenderProfile lenderProfile = getLenderProfile(lenderUserId);
        lenderProfile.setVerified(false);
        lenderProfileRepository.save(lenderProfile);
        return userService.deactivateUser(lenderUserId);
    }

    @Transactional
    public boolean permanentlyDeleteLender(Long userId) {
        userService.deleteUser(userId);
        log.warn("Lender permanently deleted. ID: {}", userId);
        return true;
    }

    public List<User> getAllLenders() {
        return userRepository.findAllByRole(Role.LENDER);
    }

    public LenderProfile getLenderProfile(Long userId) {
        return lenderProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Lender profile not found for user ID: " + userId));
    }

}
