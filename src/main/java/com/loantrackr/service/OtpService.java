package com.loantrackr.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final String OTP_PREFIX = "otp:";
    private static final String OTP_TRIES_PREFIX = "otp:tries:";
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_OTP_ATTEMPTS = 3;

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;


    public boolean generateAndSendOtp(String email) {
        log.info("Starting OTP generation process for email: {}", email);

        try {
            if (email == null || email.trim().isEmpty() || !email.matches("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")) {
                log.warn("OTP generation failed: Invalid email provided");
                return false;
            }

            String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100_000, 999_999));
            String key = OTP_PREFIX + email.trim().toLowerCase();

            redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(OTP_EXPIRY_MINUTES));
            log.debug("OTP stored in Redis for email: {} with key: {}", email, key);

            String triesKey = OTP_TRIES_PREFIX + email.trim().toLowerCase();
            redisTemplate.delete(triesKey);
            log.debug("Reset OTP attempt counter for email: {}", email);

            emailService.sendOtpEmail(email, otp);
            log.info("OTP generated and sent successfully to email: {}", email);

            return true;

        } catch (Exception e) {
            log.error("Failed to generate and send OTP for email: {}. Error: {}", email, e.getMessage(), e);

            try {
                String trimmedEmail = email != null ? email.trim().toLowerCase() : "";
                String key = OTP_PREFIX + trimmedEmail;
                String triesKey = OTP_TRIES_PREFIX + trimmedEmail;
                redisTemplate.delete(key);
                redisTemplate.delete(triesKey);
            } catch (Exception cleanupEx) {
                log.warn("Failed to cleanup Redis keys after OTP generation failure for email: {}", email);
            }

            return false;
        }
    }

    public boolean validateOtp(String email, String submittedOtp) {
        log.info("Starting OTP validation for email: {}", email);

        try {
            if (email == null || email.trim().isEmpty() || !email.matches("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")) {
                log.warn("OTP validation failed: Invalid email provided");
                return false;
            }

            if (submittedOtp == null || submittedOtp.trim().isEmpty()) {
                log.warn("OTP validation failed: Invalid OTP provided for email: {}", email);
                return false;
            }

            String normalizedEmail = email.trim().toLowerCase();
            String otpKey = OTP_PREFIX + normalizedEmail;
            String triesKey = OTP_TRIES_PREFIX + normalizedEmail;

            String storedOtp = redisTemplate.opsForValue().get(otpKey);
            if (storedOtp == null) {
                log.warn("OTP validation failed: No OTP found or OTP expired for email: {}", email);
                return false;
            }

            Long attempts = redisTemplate.opsForValue().increment(triesKey);
            redisTemplate.expire(triesKey, Duration.ofMinutes(OTP_EXPIRY_MINUTES));

            log.debug("OTP validation attempt #{} for email: {}", attempts, email);

            if (attempts != null && attempts > MAX_OTP_ATTEMPTS) {
                log.warn("OTP validation failed: Maximum attempts ({}) exceeded for email: {}", MAX_OTP_ATTEMPTS, email);

                redisTemplate.delete(otpKey);
                redisTemplate.delete(triesKey);
                log.info("OTP data cleaned up due to max attempts exceeded for email: {}", email);

                return false;
            }

            if (!storedOtp.equals(submittedOtp.trim())) {
                log.warn("OTP validation failed: Incorrect OTP provided for email: {} (Attempt: {})", email, attempts);
                return false;
            }

            redisTemplate.delete(otpKey);
            redisTemplate.delete(triesKey);
            log.info("OTP validation successful for email: {}", email);

            return true;

        } catch (Exception e) {
            log.error("Error during OTP validation for email: {}. Error: {}", email, e.getMessage(), e);
            return false;
        }
    }

    public boolean clearOtpData(String email) {
        log.info("Clearing OTP data for email: {}", email);

        try {
            if (email == null || email.trim().isEmpty() || !email.matches("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")) {
                log.warn("Cannot clear OTP data: Invalid email provided");
                return false;
            }

            String normalizedEmail = email.trim().toLowerCase();
            String otpKey = OTP_PREFIX + normalizedEmail;
            String triesKey = OTP_TRIES_PREFIX + normalizedEmail;

            redisTemplate.delete(otpKey);
            redisTemplate.delete(triesKey);

            log.info("OTP data cleared successfully for email: {}", email);
            return true;

        } catch (Exception e) {
            log.error("Failed to clear OTP data for email: {}. Error: {}", email, e.getMessage(), e);
            return false;
        }
    }
}