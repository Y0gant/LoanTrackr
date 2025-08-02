package com.loantrackr.service;

import com.loantrackr.dto.request.LenderOnboardingForm;
import com.loantrackr.dto.request.LenderUpdateRequest;
import com.loantrackr.dto.response.LenderOnboardingResponse;
import com.loantrackr.dto.response.LenderProfileResponse;
import com.loantrackr.dto.response.LenderSummaryResponse;
import com.loantrackr.enums.RequestStatus;
import com.loantrackr.exception.OperationNotAllowedException;
import com.loantrackr.exception.UserNotFoundException;
import com.loantrackr.model.LenderOnboarding;
import com.loantrackr.model.LenderProfile;
import com.loantrackr.model.User;
import com.loantrackr.repository.LenderOnboardingRepository;
import com.loantrackr.repository.LenderProfileRepository;
import com.loantrackr.util.SecurityUtils;
import com.loantrackr.util.TenureUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class LenderProfileService {

    private final LenderProfileRepository lenderProfileRepository;
    private final UserService userService;
    private final LenderOnboardingRepository lenderOnboardingRepository;
    private final FileStorageService storageService;
    private final ModelMapper modelMapper;

    @Transactional
    public LenderOnboardingResponse createLenderOnboardingApplication(LenderOnboardingForm form) {
        Optional<User> userByEmail = userService.getUserByEmail(form.getEmail());
        if (userByEmail.isPresent()) {
            throw new OperationNotAllowedException("A user already exists with email :" + form.getEmail());
        }
        String gstCertificate = storageService.storeFile(form.getGstCertificate(), "gst_certificate");
        String panCards = storageService.storeFile(form.getPanCard(), "pan_cards");
        String rbiLicenses = storageService.storeFile(form.getRbiLicense(), "rbi_licenses");

        LenderOnboarding onboarding = LenderOnboarding.builder()
                .username(form.getUsername())
                .email(form.getEmail())
                .gstin(form.getGstin())
                .rbiLicenseNumber(form.getRbiLicenseNumber())
                .organizationName(form.getOrganizationName())
                .contactPersonName(form.getContactPersonName())
                .gstCertificate(gstCertificate)
                .panCard(panCards)
                .rbiLicense(rbiLicenses)
                .reviewed(false)
                .status(RequestStatus.PENDING)
                .build();
        LenderOnboarding saved = lenderOnboardingRepository.save(onboarding);
        return modelMapper.map(saved, LenderOnboardingResponse.class);
    }

    @Transactional
    public LenderProfile createProfileFromRequest(LenderOnboarding request, User lenderUser) {
        LenderProfile lenderProfile = LenderProfile.builder()
                .user(lenderUser)
                .gstin(request.getGstin())
                .isVerified(true)
                .interestRate(new BigDecimal(BigInteger.ONE))
                .organizationName(request.getOrganizationName())
                .rbiLicenseNumber(request.getRbiLicenseNumber())
                .supportedTenures("6")
                .processingFee(new BigDecimal("500"))
                .build();
        return lenderProfileRepository.save(lenderProfile);
    }

    public LenderProfile getLenderById(Long id) {
        Optional<LenderProfile> lenderProfileOptional = lenderProfileRepository.findById(id);
        if (lenderProfileOptional.isEmpty()) throw new UserNotFoundException("No lender found for id: " + id);
        return lenderProfileOptional.get();
    }

    public LenderProfileResponse getCurrentLender() {
        String userName = SecurityUtils.getCurrentUserName();
        Optional<User> userByUserName = userService.getUserByUserName(userName);
        if (userByUserName.isEmpty()) throw new UserNotFoundException("No user found for username: " + userName);
        return mapToResponse(getLenderById(userByUserName.get().getId()));
    }


    @Transactional
    public LenderSummaryResponse updateLenderProfile(LenderUpdateRequest request) {
        String userName = SecurityUtils.getCurrentUserName();
        Optional<User> userByUserName = userService.getUserByUserName(userName);
        if (userByUserName.isEmpty()) throw new UserNotFoundException("No user found for user name: " + userName);
        LenderProfile byUser = lenderProfileRepository.findByUser(userByUserName.get());
        if (StringUtils.hasText(request.getOrganizationName())) {
            byUser.setOrganizationName(request.getOrganizationName());
        }
        if (StringUtils.hasText(request.getGstin())) {
            byUser.setGstin(request.getGstin());
        }
        if (StringUtils.hasText(request.getRbiLicenseNumber())) {
            byUser.setRbiLicenseNumber(request.getRbiLicenseNumber());
        }
        if (request.getInterestRate() != null) {
            byUser.setInterestRate(request.getInterestRate());
        }
        if (StringUtils.hasText(request.getSupportedTenures())) {
            byUser.setSupportedTenures(request.getSupportedTenures());
        }
        if (request.getProcessingFees() != null) {
            byUser.setProcessingFee(request.getProcessingFees());
        }
        byUser = lenderProfileRepository.save(byUser);
        return LenderSummaryResponse.builder()
                .lenderId(byUser.getId())
                .organizationName(byUser.getOrganizationName())
                .supportedTenures(TenureUtils.parseSupportedTenures(byUser.getSupportedTenures()))
                .interestRate(byUser.getInterestRate())
                .processingFee(byUser.getProcessingFee())
                .build();
    }

    @Transactional
    public boolean deleteLender() {
        String userName = SecurityUtils.getCurrentUserName();
        Optional<User> userByUserName = userService.getUserByUserName(userName);
        if (userByUserName.isEmpty()) {
            throw new UserNotFoundException("No lender found to delete with username: " + userName);
        }
        try {

            LenderProfile byUser = lenderProfileRepository.findByUser(userByUserName.get());
            byUser.setVerified(false);
            lenderProfileRepository.save(byUser);
            return userService.deleteUser(byUser.getId());

        } catch (RuntimeException e) {
            log.error("Unable to delete lender with username : {}", userName);
            throw e;
        }
    }

    public LenderProfileResponse mapToResponse(LenderProfile lenderProfile) {
        User user = lenderProfile.getUser();

        return LenderProfileResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .isEmailVerified(user.isEmailVerified())
                .isActive(user.isActive())

                .gstin(lenderProfile.getGstin())
                .rbiLicenseNumber(lenderProfile.getRbiLicenseNumber())
                .organizationName(lenderProfile.getOrganizationName())
                .isVerified(lenderProfile.isVerified())
                .interestRate(lenderProfile.getInterestRate())
                .processingFee(lenderProfile.getProcessingFee())
                .supportedTenures(lenderProfile.getSupportedTenures())
                .build();
    }


}