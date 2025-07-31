package com.loantrackr.service;

import com.loantrackr.dto.request.KycUpdateRequest;
import com.loantrackr.dto.request.RegisterBorrowerRequest;
import com.loantrackr.dto.request.UpdateUserRequest;
import com.loantrackr.dto.response.UserResponse;
import com.loantrackr.exception.OperationNotAllowedException;
import com.loantrackr.exception.UserNotFoundException;
import com.loantrackr.model.BankDetails;
import com.loantrackr.model.BorrowerKycDetails;
import com.loantrackr.model.User;
import com.loantrackr.repository.BankDetailsRepository;
import com.loantrackr.repository.BorrowerKycDetailsRepository;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.loantrackr.util.SecurityUtils.getCurrentUserName;

@Service
@AllArgsConstructor
public class BorrowerService {
    private final UserService userService;
    private final ModelMapper mapper;
    private final BorrowerKycDetailsRepository kycDetails;
    private final BankDetailsRepository bankDetailsRepository;
    private final LoanService loanService;

    public UserResponse createBorrower(RegisterBorrowerRequest userToRegister) {

        User user = userService.createUser(userToRegister);


        BorrowerKycDetails userKyc = BorrowerKycDetails.builder()
                .user(user)
                .city(userToRegister.getCity())
                .state(userToRegister.getState())
                .address(userToRegister.getAddress())
                .pincode(userToRegister.getPincode())
                .dateOfBirth(userToRegister.getDateOfBirth())
                .employmentType(userToRegister.getEmploymentType())
                .aadhaarNumber(userToRegister.getAadhaarNumber())
                .panNumber(userToRegister.getPanNumber())
                .isKycVerified(false)
                .build();
        kycDetails.save(userKyc);

        BankDetails bankDetails = BankDetails.builder()
                .user(user)
                .accountHolderName(userToRegister.getAccountHolderName())
                .accountNumber(userToRegister.getAccountNumber())
                .ifscCode(userToRegister.getIfscCode())
                .bankName(userToRegister.getBankName())
                .branchName(userToRegister.getBranchName())
                .upiId(userToRegister.getUpiId()) // nullable
                .isAccountVerified(false)
                .build();
        bankDetailsRepository.save(bankDetails);

        return mapper.map(user, UserResponse.class);
    }


    public UserResponse getInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> userByUserName = userService.getUserByUserName(authentication.getName());
        if (userByUserName.isEmpty()) throw new UserNotFoundException("No user found");
        return mapper.map(userByUserName.get(), UserResponse.class);
    }


    public BorrowerKycDetails getKycInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> userByUserName = userService.getUserByUserName(authentication.getName());
        if (userByUserName.isEmpty()) throw new UserNotFoundException("No user found");
        Optional<BorrowerKycDetails> byId = kycDetails.findById(userByUserName.get().getId());
        if (byId.isEmpty()) throw new UserNotFoundException("No user found");
        return byId.get();
    }

    @Transactional
    public BorrowerKycDetails updateKycInfo(KycUpdateRequest request) {
        BorrowerKycDetails borrowerKycDetails = getKycInfo();

        boolean kycReset = false;

        if (request.getPanNumber() != null && !request.getPanNumber().isBlank()
                && !request.getPanNumber().equals(borrowerKycDetails.getPanNumber())) {
            borrowerKycDetails.setPanNumber(request.getPanNumber());
            kycReset = true;
        }

        if (request.getAadhaarNumber() != null && !request.getAadhaarNumber().isBlank()
                && !request.getAadhaarNumber().equals(borrowerKycDetails.getAadhaarNumber())) {
            borrowerKycDetails.setAadhaarNumber(request.getAadhaarNumber());
            kycReset = true;
        }

        if (request.getDateOfBirth() != null && !request.getDateOfBirth().equals(borrowerKycDetails.getDateOfBirth())) {
            borrowerKycDetails.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getAddress() != null && !request.getAddress().isBlank()
                && !request.getAddress().equals(borrowerKycDetails.getAddress())) {
            borrowerKycDetails.setAddress(request.getAddress());
        }

        if (request.getPincode() != null && !request.getPincode().isBlank()
                && !request.getPincode().equals(borrowerKycDetails.getPincode())) {
            borrowerKycDetails.setPincode(request.getPincode());
        }

        if (request.getCity() != null && !request.getCity().isBlank()
                && !request.getCity().equals(borrowerKycDetails.getCity())) {
            borrowerKycDetails.setCity(request.getCity());
        }

        if (request.getState() != null && !request.getState().isBlank()
                && !request.getState().equals(borrowerKycDetails.getState())) {
            borrowerKycDetails.setState(request.getState());
        }

        if (request.getEmploymentType() != null && !request.getEmploymentType().isBlank()
                && !request.getEmploymentType().equals(borrowerKycDetails.getEmploymentType())) {
            borrowerKycDetails.setEmploymentType(request.getEmploymentType());
        }

        if (kycReset) {
            borrowerKycDetails.setKycVerified(false);
        }

        return kycDetails.save(borrowerKycDetails);
    }


    public UserResponse updateUser(UpdateUserRequest request) {
        String userName = getCurrentUserName();
        Optional<User> userByUserName = userService.getUserByUserName(userName);
        if (userByUserName.isEmpty()) throw new UserNotFoundException("No user found");
        User user = userService.updateUser(userByUserName.get().getId(), request);
        return mapper.map(user, UserResponse.class);
    }

    @Transactional
    public boolean deleteBorrower() {
        String userName = getCurrentUserName();
        Optional<User> userByUserName = userService.getUserByUserName(userName);
        if (userByUserName.isEmpty()) throw new UserNotFoundException("No user found");
        if (loanService.userHasActiveLoan(userByUserName.get().getId())) {
            throw new OperationNotAllowedException("User has active loan cannot delete account.");
        }
        Optional<BorrowerKycDetails> byId = kycDetails.findById(userByUserName.get().getId());
        if (byId.isPresent()) {
            BorrowerKycDetails borrowerKycDetails = byId.get();
            borrowerKycDetails.setKycVerified(false);
            kycDetails.save(borrowerKycDetails);
        }
        Optional<BankDetails> byId1 = bankDetailsRepository.findById(userByUserName.get().getId());
        if (byId1.isPresent()) {
            BankDetails bankDetails = byId1.get();
            bankDetails.setAccountVerified(false);
            bankDetailsRepository.save(bankDetails);
        }
        return userService.deleteUser(userByUserName.get().getId());
    }

}