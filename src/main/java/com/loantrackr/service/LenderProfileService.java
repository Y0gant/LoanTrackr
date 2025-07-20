package com.loantrackr.service;

import com.loantrackr.dto.request.LenderUpdateRequest;
import com.loantrackr.dto.response.LenderSummaryResponse;
import com.loantrackr.exception.UserNotFoundException;
import com.loantrackr.model.LenderOnboarding;
import com.loantrackr.model.LenderProfile;
import com.loantrackr.model.User;
import com.loantrackr.repository.LenderProfileRepository;
import com.loantrackr.util.SecurityUtils;
import com.loantrackr.util.TenureUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            userService.deleteUser(byUser.getId());

        } catch (RuntimeException e) {
            log.error("Unable to delete lender with username : {}", userName);
            throw e;
        }
        return true;
    }

}