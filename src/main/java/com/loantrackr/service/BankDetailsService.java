package com.loantrackr.service;

import com.loantrackr.dto.request.BankDetailsUpdateRequest;
import com.loantrackr.enums.Role;
import com.loantrackr.exception.UnauthorizedException;
import com.loantrackr.exception.UserNotFoundException;
import com.loantrackr.model.BankDetails;
import com.loantrackr.model.User;
import com.loantrackr.repository.BankDetailsRepository;
import com.loantrackr.util.SecurityUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.Set;

@Data
@Service
@AllArgsConstructor
public class BankDetailsService {
    private final BankDetailsRepository bankDetailsRepository;
    private final UserService userService;

    @Transactional
    public BankDetails updateBankDetails(BankDetailsUpdateRequest request) {
        String userName = SecurityUtils.getCurrentUserName();
        Optional<User> userByUserNameOptional = userService.getUserByUserName(userName);

        if (userByUserNameOptional.isEmpty()) {
            throw new UserNotFoundException("No User found for username: " + userName);
        }

        User user = userByUserNameOptional.get();

        Set<Role> allowedRoles = Set.of(Role.BORROWER, Role.LENDER);
        if (!allowedRoles.contains(user.getRole())) {
            throw new UnauthorizedException("Cannot update bank details for role: " + user.getRole() + " | user: " + userName);
        }

        BankDetails bankDetails = bankDetailsRepository.findById(user.getId())
                .orElseThrow();


        if (StringUtils.hasText(request.getAccountHolderName())) {
            bankDetails.setAccountHolderName(request.getAccountHolderName());
        }

        if (StringUtils.hasText(request.getAccountNumber())) {
            bankDetails.setAccountNumber(request.getAccountNumber());
        }

        if (StringUtils.hasText(request.getIfscCode())) {
            bankDetails.setIfscCode(request.getIfscCode());
        }

        if (StringUtils.hasText(request.getBankName())) {
            bankDetails.setBankName(request.getBankName());
        }

        if (StringUtils.hasText(request.getBranchName())) {
            bankDetails.setBranchName(request.getBranchName());
        }

        if (StringUtils.hasText(request.getUpiId())) {
            bankDetails.setUpiId(request.getUpiId());
        }
        return bankDetailsRepository.save(bankDetails);
    }

    public BankDetails getBankDetails() {
        String userName = SecurityUtils.getCurrentUserName();
        Optional<User> userByUserName = userService.getUserByUserName(userName);
        if (userByUserName.isEmpty()) {
            throw new UserNotFoundException("No user found for username :" + userName);
        }
        User user = userByUserName.get();
        Set<Role> allowedRoles = Set.of(Role.BORROWER, Role.LENDER);
        if (!allowedRoles.contains(user.getRole())) {
            throw new UnauthorizedException("Cannot get bank details for role: " + user.getRole() + " | user: " + userName);
        }
        Optional<BankDetails> byId = bankDetailsRepository.findById(user.getId());
        return byId.get();
    }

    @Transactional
    public boolean refuteBankDetails(Long id) {
        Optional<BankDetails> byId = bankDetailsRepository.findById(id);
        if (byId.isEmpty()) throw new UserNotFoundException("No bank details found for id :" + id);
        BankDetails bankDetails = byId.get();
        bankDetails.setAccountVerified(false);
        bankDetailsRepository.save(bankDetails);
        return true;
    }

}
