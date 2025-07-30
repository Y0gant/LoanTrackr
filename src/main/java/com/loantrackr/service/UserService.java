package com.loantrackr.service;

import com.loantrackr.dto.request.LoginRequest;
import com.loantrackr.dto.request.RegisterBorrowerRequest;
import com.loantrackr.dto.request.RegisterUser;
import com.loantrackr.dto.request.UpdateUserRequest;
import com.loantrackr.enums.AuthProvider;
import com.loantrackr.enums.Role;
import com.loantrackr.exception.*;
import com.loantrackr.model.User;
import com.loantrackr.repository.UserRepository;
import com.loantrackr.security.jwt.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtils;

    //Core CRUD Ops

    @Transactional
    public User createUser(RegisterBorrowerRequest request) {
        log.info("START: Creating borrower user: {}", request.getEmail());
        try {
            User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail().toLowerCase())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(Role.BORROWER)
                    .provider(AuthProvider.LOCAL)
                    .isVerified(true)
                    .isEmailVerified(true)
                    .isActive(true)
                    .build();
            User saved = userRepository.save(user);
            log.info("SUCCESS: Created borrower user with ID: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("ERROR: Failed to create borrower user: {}", request.getEmail(), e);
            throw e;
        }
    }

    @Transactional
    public User createPrivilegedUser(RegisterUser request, Role role) {
        log.info("START: Creating privileged user. Email: {}, Role: {}", request.getEmail(), role);
        try {
            User user = modelMapper.map(request, User.class);
            user.setEmail(user.getEmail().toLowerCase());
            user.setProvider(AuthProvider.LOCAL);
            user.setRole(role);
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            User saved = userRepository.save(user);
            log.info("SUCCESS: Created privileged user with ID: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("ERROR: Failed to create privileged user: {}", request.getEmail(), e);
            throw e;
        }
    }

    @Transactional
    public User updateUser(Long id, UpdateUserRequest userRequest) {
        log.info("START: Updating user with ID: {}", id);
        User user = userRepository.findUserById(id)
                .orElseThrow(() -> {
                    log.warn("WARN: Update failed. User not found. ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                });

        validateActiveAndNotDeleted(user);
        boolean isUpdated = false;
        if (userRequest.getUsername() != null && !userRequest.getUsername().isBlank()) {
            user.setUsername(userRequest.getUsername());
            isUpdated = true;
        }

        if (userRequest.getPassword() != null && !userRequest.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
            isUpdated = true;
        }

        if (userRequest.getEmail() != null && !userRequest.getEmail().isBlank()) {
            if (!user.getEmail().equals(userRequest.getEmail())) {
                user.setEmailVerified(false);
            }
            user.setEmail(userRequest.getEmail().toLowerCase());
            isUpdated = true;
        }
        if (isUpdated) {
            user.setUpdatedAt(LocalDateTime.now());
        }
        log.info("SUCCESS: Updated user with ID: {}", id);
        return userRepository.save(user);
    }

    @Transactional
    public boolean deleteUser(Long id) {
        log.warn("START: Soft-deleting user. ID: {}", id);
        User user = userRepository.findUserById(id)
                .orElseThrow(() -> {
                    log.warn("WARN: Delete failed. User not found. ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                });

        user.setActive(false);
        user.setEmailVerified(false);
        user.setVerified(false);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        log.warn("SUCCESS: Soft-deleted user. ID: {}", id);
        return true;
    }

    // Read ops

    public Optional<User> getUserByID(Long id) {
        log.debug("Fetching user by ID: {}", id);
        return userRepository.findUserById(id);
    }

    public Optional<User> getUserByUserName(String userName) {
        log.debug("Fetching user by username: {}", userName);
        return userRepository.findUserByUsername(userName);
    }

    public Optional<User> getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    // Email Verification

    public boolean isEmailVerified(Long id) {
        log.info("Checking email verification status. User ID: {}", id);
        User user = getUserByID(id)
                .orElseThrow(() -> {
                    log.warn("WARN: Email verification check failed. User not found. ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                });

        validateActiveAndNotDeleted(user);
        return user.isEmailVerified();
    }

    @Transactional
    public boolean markEmailVerified(Long id) {
        log.info("Marking email verified. User ID: {}", id);
        User user = getUserByID(id)
                .orElseThrow(() -> {
                    log.warn("WARN: Mark email verified failed. User not found. ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                });

        validateActiveAndNotDeleted(user);
        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("SUCCESS: Email marked as verified for user ID: {}", id);
        return true;
    }

    public boolean isActive(Long id) {
        return getUserByID(id)
                .orElseThrow(() -> {
                    log.warn("WARN: isActive check failed. User not found. ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                }).isActive();
    }

    @Transactional
    public boolean deactivateUser(Long id) {
        log.warn("START: Deactivating user. ID: {}", id);
        User user = getUserByID(id)
                .orElseThrow(() -> {
                    log.warn("WARN: Deactivate failed. User not found. ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                });

        if (user.isPermanentlyDeleted()) {
            log.warn("WARN: Attempt to deactivate permanently deleted user. ID: {}", id);
            throw new UserPermanentlyDeletedException("Cannot deactivate a permanently deleted user.");
        }

        if (user.isActive()) {
            user.setActive(false);
            userRepository.save(user);
            log.warn("SUCCESS: User deactivated. ID: {}", id);
            return true;
        }

        log.info("SKIPPED: User already inactive. ID: {}", id);
        return false;
    }

    public boolean isVerified(Long id) {
        return getUserByID(id)
                .orElseThrow(() -> {
                    log.warn("WARN: isVerified check failed. User not found. ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                }).isVerified();
    }

    @Transactional
    public boolean markUserVerified(Long id) {
        log.info("Marking user as verified. ID: {}", id);
        User user = getUserByID(id).orElseThrow(() -> {
            log.warn("WARN: Mark verified failed. User not found. ID: {}", id);
            return new UserNotFoundException("User not found with ID: " + id);
        });

        validateActiveAndNotDeleted(user);
        user.setVerified(true);
        userRepository.save(user);
        log.info("SUCCESS: User marked as verified. ID: {}", id);
        return true;
    }

    @Transactional
    public boolean activateUser(Long id) {
        log.warn("START: Activating user. ID: {}", id);
        User user = getUserByID(id)
                .orElseThrow(() -> {
                    log.warn("WARN: Activate failed. User not found. ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                });

        if (user.isPermanentlyDeleted()) {
            log.warn("WARN: Attempt to activate permanently deleted user. ID: {}", id);
            throw new UserPermanentlyDeletedException("Cannot activate a permanently deleted user.");
        }

        if (!user.isActive()) {
            user.setActive(true);
            user.setDeletedAt(null);
            userRepository.save(user);
            log.info("SUCCESS: User activated. ID: {}", id);
            return true;
        }

        log.info("SKIPPED: User already active. ID: {}", id);
        return false;
    }

    public boolean hasRole(Long id, Role role) {
        return getUserByID(id)
                .orElseThrow(() -> {
                    log.warn("WARN: Role check failed. User not found. ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                })
                .getRole().equals(role);
    }

    // Utility

    public boolean existsByEmail(String email) {
        log.debug("Checking if email exists: {}", email);
        return userRepository.existsByEmail(email);
    }

    public boolean existsByUserName(String userName) {
        log.debug("Checking if username exists: {}", userName);
        return userRepository.existsByUsername(userName);
    }

    public boolean existsByRole(Role role) {
        log.debug("Checking if any user exists with role: {}", role);
        return userRepository.existsByRole(role);
    }

    // Helpers

    private void validateActiveAndNotDeleted(User user) {
        if (!user.isActive()) {
            log.warn("WARN: User is inactive. ID={}", user.getId());
            throw new InactiveUserException("User is inactive.");
        }
        if (user.isPermanentlyDeleted()) {
            log.warn("WARN: User is permanently deleted. ID={}", user.getId());
            throw new UserPermanentlyDeletedException("User is permanently deleted.");
        }
    }

    public Optional<User> authenticate(String identifier, String password) {
        log.info("START: Authentication attempt for identifier: {}", identifier);

        Optional<User> optionalUser = userRepository.findUserByUsernameOrEmail(identifier);

        if (optionalUser.isEmpty()) {
            log.warn("WARN: Authentication failed. No user found. Identifier: {}", identifier);
            throw new UserNotFoundException("Invalid username/email or password.");
        }

        User user = optionalUser.get();

        validateActiveAndNotDeleted(user);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("WARN: Authentication failed. Invalid password. User ID: {}", user.getId());
            throw new InvalidCredentialsException("Invalid username/email or password.");
        }

        log.info("SUCCESS: Authentication successful. User ID: {}", user.getId());
        return Optional.of(user);
    }

    public void validatePrivilegedAccess(User user) {
        if (EnumSet.of(Role.LENDER, Role.SYSTEM_ADMIN, Role.LOAN_MANAGER).contains(user.getRole())
                && !user.isVerified()) {
            log.warn("WARN: Unauthorized privileged access attempt. User ID: {}", user.getId());
            throw new UnauthorizedException("Privileged account is not verified yet.");
        }
    }

    @Transactional
    public String generateJwtForLogin(LoginRequest loginUser) {
        log.info("START: Generating JWT for login. Identifier: {}", loginUser.getIdentifier());
        Optional<User> user = userRepository.findUserByUsernameOrEmail(loginUser.getIdentifier());
        if (user.isEmpty()) {
            log.error("ERROR: JWT generation failed. User not found: {}", loginUser.getIdentifier());
            throw new UsernameNotFoundException("User not found: " + loginUser.getIdentifier());
        }
        User user1 = user.get();
        user1.setLastLogin(LocalDateTime.now());
        userRepository.save(user1);
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user1.getId());
        claims.put("email", user1.getEmail());
        claims.put("isVerified", user1.isVerified());
        claims.put("isEmailVerified", user1.isEmailVerified());
        claims.put("roles", user1.getRole());
        log.info("SUCCESS: JWT generated for user ID: {}", user1.getId());
        return jwtUtils.generateToken(user1.getUsername(), claims);
    }
}
