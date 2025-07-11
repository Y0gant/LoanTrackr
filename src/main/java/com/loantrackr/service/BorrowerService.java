package com.loantrackr.service;

import com.loantrackr.dto.request.KycUpdateRequest;
import com.loantrackr.dto.request.RegisterUser;
import com.loantrackr.dto.request.UpdateUserRequest;
import com.loantrackr.dto.response.UserResponse;
import com.loantrackr.exception.UserNotFoundException;
import com.loantrackr.model.BorrowerKycDetails;
import com.loantrackr.model.User;
import com.loantrackr.repository.BorrowerKycDetailsRepository;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@AllArgsConstructor
public class BorrowerService {
    private final UserService userService;
    private final ModelMapper mapper;
    private final BorrowerKycDetailsRepository kycDetails;

    @Transactional
    public UserResponse createBorrower(RegisterUser userToRegister) {
        User user = userService.createUser(userToRegister);
        BorrowerKycDetails userKyc = new BorrowerKycDetails();
        userKyc.setUser(user);
        kycDetails.save(userKyc);
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
        if (request.getPanNumber() != null && !request.getPanNumber().isBlank() && !request.getPanNumber().equals(borrowerKycDetails.getPanNumber())) {
            borrowerKycDetails.setPanNumber(request.getPanNumber());
            borrowerKycDetails.setKycVerified(false);
        }
        if (request.getAadhaarNumber() != null && !request.getAadhaarNumber().isBlank() && !request.getAadhaarNumber().equals(borrowerKycDetails.getAadhaarNumber())) {
            borrowerKycDetails.setAadhaarNumber(request.getAadhaarNumber());
            borrowerKycDetails.setKycVerified(false);
        }
        return kycDetails.save(borrowerKycDetails);
    }


    public UserResponse updateUser(UpdateUserRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> userByUserName = userService.getUserByUserName(authentication.getName());
        if (userByUserName.isEmpty()) throw new UserNotFoundException("No user found");
        User user = userService.updateUser(userByUserName.get().getId(), request);
        return mapper.map(user, UserResponse.class);
    }

    public boolean deleteBorrower() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> userByUserName = userService.getUserByUserName(authentication.getName());
        if (userByUserName.isEmpty()) throw new UserNotFoundException("No user found");
        return userService.deleteUser(userByUserName.get().getId());
    }

}