package com.loantrackr.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final String OTP_PREFIX = "otp:";
    private static final String OTP_TRIES_PREFIX = "otp:tries:";

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    public void generateAndSendOtp(String email) {
        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100_000, 999_999));
        String key = OTP_PREFIX + email;

        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(10));

        redisTemplate.delete(OTP_TRIES_PREFIX + email);

        emailService.sendOtpEmail(email, otp);
    }

    public boolean validateOtp(String email, String submittedOtp) {
        String otpKey = OTP_PREFIX + email;
        String triesKey = OTP_TRIES_PREFIX + email;

        String storedOtp = redisTemplate.opsForValue().get(otpKey);
        if (storedOtp == null) return false;

        Long tries = redisTemplate.opsForValue().increment(triesKey);
        redisTemplate.expire(triesKey, Duration.ofMinutes(10));

        if (tries != null && tries >= 3) {
            redisTemplate.delete(otpKey);
            redisTemplate.delete(triesKey);
            return false;
        }

        if (!storedOtp.equals(submittedOtp)) {
            return false;
        }

        redisTemplate.delete(otpKey);
        redisTemplate.delete(triesKey);
        return true;
    }
}
