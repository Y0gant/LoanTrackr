package com.loantrackr.service;

import com.loantrackr.dto.request.RegisterUser;
import com.loantrackr.dto.request.UpdateUserRequest;
import com.loantrackr.enums.AuthProvider;
import com.loantrackr.enums.Role;
import com.loantrackr.exception.*;
import com.loantrackr.model.User;
import com.loantrackr.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    //Core CRUD Ops

    @Transactional
    public User createUser(RegisterUser request) {
        log.info("Attempting to create borrower user: {}", request.getEmail());
        try {
            User user = modelMapper.map(request, User.class);
            user.setRole(Role.BORROWER);
            user.setProvider(AuthProvider.LOCAL);
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            User saved = userRepository.save(user);
            log.info("Successfully created user with ID: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("Error creating user: {}", request.getEmail(), e);
            throw e;
        }
    }

    @Transactional
    public User createPrivilegedUser(RegisterUser request, Role role) {
        log.info("Creating privileged user with role: {} and email: {}", role, request.getEmail());
        try {
            User user = modelMapper.map(request, User.class);
            user.setProvider(AuthProvider.LOCAL);
            user.setRole(role);
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            User saved = userRepository.save(user);
            log.info("Successfully created privileged user with ID: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("Error creating privileged user: {}", request.getEmail(), e);
            throw e;
        }
    }

    @Transactional
    public User updateUser(Long id, UpdateUserRequest userRequest) {
        User user = userRepository.findUserById(id)
                .orElseThrow(() -> {
                    log.warn("Update failed. User not found with ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                });
        validateActiveAndNotDeleted(user);
        if (userRequest.getUsername() != null && !userRequest.getUsername().isBlank()) {
            user.setUsername(userRequest.getUsername());
        }

        if (userRequest.getPassword() != null && !userRequest.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        }

        if (userRequest.getEmail() != null && !userRequest.getEmail().isBlank()) {
            user.setEmail(userRequest.getEmail());
        }

        if (userRequest.getIsActive() != null && userRequest.getIsActive() != user.isActive()) {
            user.setActive(userRequest.getIsActive());
        }

        log.info("User updated successfully. ID: {}", id);
        return userRepository.save(user);
    }

    @Transactional
    public boolean deleteUser(Long id) {
        User user = userRepository.findUserById(id)
                .orElseThrow(() -> {
                    log.warn("Delete failed. User not found with ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                });
        user.setActive(false);
        user.setEmailVerified(false);
        user.setVerified(false);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        log.warn("Soft-deleted user. ID: {}", id);
        return true;
    }


    // Read ops

    public Optional<User> getUserByID(Long id) {
        return userRepository.findUserById(id);
    }

    public Optional<User> getUserByUserName(String userName) {
        return userRepository.findUserByUsername(userName);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }


    // Email Verification

    public boolean isEmailVerified(Long id) {
        User user = getUserByID(id)
                .orElseThrow(() -> {
                    log.warn("Email verification check failed. User not found. ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                });

        validateActiveAndNotDeleted(user);

        return user.isEmailVerified();
    }

    @Transactional
    public boolean markEmailVerified(Long id) {
        User user = getUserByID(id)
                .orElseThrow(() -> {
                    log.warn("Mark email verified failed. User not found. ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                });

        validateActiveAndNotDeleted(user);

        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Email marked as verified for user ID: {}", id);
        return true;
    }


    public boolean isActive(Long id) {
        return getUserByID(id)
                .orElseThrow(() -> {
                    log.warn("isActive check failed. User not found. ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                }).isActive();
    }

    @Transactional
    public boolean deactivateUser(Long id) {
        User user = getUserByID(id)
                .orElseThrow(() -> {
                    log.warn("Deactivate failed. User not found. ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                });

        if (user.isPermanentlyDeleted()) {
            log.warn("Attempt to deactivate permanently deleted user. ID: {}", id);
            throw new UserPermanentlyDeletedException("Cannot deactivate a permanently deleted user.");
        }

        if (user.isActive()) {
            user.setActive(false);
            user.setDeletedAt(LocalDateTime.now());
            userRepository.save(user);
            log.warn("User deactivated. ID: {}", id);
            return true;
        }

        log.info("Deactivate skipped. User already inactive. ID: {}", id);
        return false;
    }

    public boolean isVerified(Long id) {
        return getUserByID(id)
                .orElseThrow(() -> {
                    log.warn("isVerified check failed. User not found. ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                }).isVerified();
    }

    @Transactional
    public boolean markUserVerified(Long id) {
        User user = getUserByID(id).orElseThrow(() -> {
            log.warn("Mark verified failed. User not found. ID: {}", id);
            return new UserNotFoundException("User not found with ID: " + id);
        });

        validateActiveAndNotDeleted(user);

        user.setVerified(true);
        userRepository.save(user);
        log.info("User marked as verified. ID: {}", id);
        return true;
    }


    @Transactional
    public boolean activateUser(Long id) {
        User user = getUserByID(id)
                .orElseThrow(() -> {
                    log.warn("Activate failed. User not found. ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                });

        if (user.isPermanentlyDeleted()) {
            log.warn("Attempt to activate permanently deleted user. ID: {}", id);
            throw new UserPermanentlyDeletedException("Cannot activate a permanently deleted user.");
        }

        if (!user.isActive()) {
            user.setActive(true);
            user.setDeletedAt(null);
            userRepository.save(user);
            log.info("User activated. ID: {}", id);
            return true;
        }

        log.info("Activate skipped. User already active. ID: {}", id);
        return false;
    }

    public boolean hasRole(Long id, Role role) {
        return getUserByID(id)
                .orElseThrow(() -> {
                    log.warn("Role check failed. User not found. ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                })
                .getRole().equals(role);
    }

    // Utility

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByUserName(String userName) {
        return userRepository.existsByUsername(userName);
    }

    public boolean existsByRole(Role role) {
        return userRepository.existsByRole(role);
    }

    // Helpers

    private void validateActiveAndNotDeleted(User user) {
        if (!user.isActive()) {
            log.warn("User is inactive: ID={}", user.getId());
            throw new InactiveUserException("User is inactive.");
        }
        if (user.isPermanentlyDeleted()) {
            log.warn("User is permanently deleted: ID={}", user.getId());
            throw new UserPermanentlyDeletedException("User is permanently deleted.");
        }
    }

    public Optional<User> authenticate(String identifier, String password) {
        log.info("Authentication attempt for identifier: {}", identifier);

        Optional<User> optionalUser = userRepository.findUserByUsernameOrEmail(identifier, identifier);

        if (optionalUser.isEmpty()) {
            log.warn("Authentication failed. No user found with identifier: {}", identifier);
            throw new UserNotFoundException("Invalid username/email or password.");
        }

        User user = optionalUser.get();

        validateActiveAndNotDeleted(user);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Authentication failed. Invalid password for user ID: {}", user.getId());
            throw new InvalidCredentialsException("Invalid username/email or password.");
        }

        log.info("Authentication successful for user ID: {}", user.getId());
        return Optional.of(user);
    }


    public void validatePrivilegedAccess(User user) {
        if (EnumSet.of(Role.LENDER, Role.SYSTEM_ADMIN, Role.LOAN_MANAGER, Role.LOAN_OFFICER).contains(user.getRole())
                && !user.isVerified()) {
            throw new UnauthorizedException("Privileged account is not verified yet.");
        }
    }


}
