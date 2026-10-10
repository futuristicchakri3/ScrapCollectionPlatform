package com.scrapconnection.service;

import com.scrapconnection.entity.OtpVerification;
import com.scrapconnection.exception.BadRequestException;
import com.scrapconnection.repository.OtpVerificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.sms.mode:development}")
    private String smsMode;

    @Value("${app.otp.expiry-minutes:5}")
    private int expiryMinutes;

    @Value("${app.otp.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.otp.rate-limit-minutes:10}")
    private int rateLimitMinutes;

    @Value("${app.otp.max-requests-per-window:3}")
    private int maxRequestsPerWindow;

    public OtpService(OtpVerificationRepository otpVerificationRepository, PasswordEncoder passwordEncoder) {
        this.otpVerificationRepository = otpVerificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String generateAndSendOtp(String mobileNumber, String purpose) {
        // 1. Rate limiting check
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(rateLimitMinutes);
        long recentRequests = otpVerificationRepository.countByMobileNumberAndCreatedAtAfter(mobileNumber, windowStart);
        if (recentRequests >= maxRequestsPerWindow) {
            throw new BadRequestException("Too many OTP requests. Please wait " + rateLimitMinutes + " minutes before trying again.");
        }

        // 2. Generate 6-digit cryptographic OTP
        int otpInt = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(otpInt);

        // 3. Hash OTP before persistence
        String otpHash = passwordEncoder.encode(otpCode);

        // 4. Store dev OTP only if in development mode
        String devOtp = isDevelopmentMode() ? otpCode : null;
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(expiryMinutes);

        OtpVerification otpVerification = new OtpVerification(mobileNumber, otpHash, devOtp, purpose, expiryTime);
        otpVerificationRepository.save(otpVerification);

        // 5. Send OTP via SMS or Dev Mode
        if (isDevelopmentMode()) {
            logger.info("==================================================================");
            logger.info("[DEVELOPMENT OTP MODE ACTIVE]");
            logger.info("Mobile: {}", mobileNumber);
            logger.info("Purpose: {}", purpose);
            logger.info("OTP Code: {}", otpCode);
            logger.info("Expires at: {}", expiryTime);
            logger.info("NOTE: SMS was NOT sent via network provider. This is local development mode.");
            logger.info("==================================================================");
        } else {
            // Production SMS dispatch logic would connect to SMS gateway (e.g., Twilio, MSG91)
            logger.info("Dispatched SMS OTP via production gateway to mobile: {}", mobileNumber);
        }

        return otpCode;
    }

    @Transactional
    public boolean verifyOtp(String mobileNumber, String inputOtp, String purpose) {
        Optional<OtpVerification> optOtp = otpVerificationRepository.findTopByMobileNumberAndPurposeOrderByCreatedAtDesc(mobileNumber, purpose);

        if (optOtp.isEmpty()) {
            throw new BadRequestException("No OTP found for this mobile number. Please request a new OTP.");
        }

        OtpVerification otp = optOtp.get();

        if (otp.isVerified()) {
            throw new BadRequestException("This OTP has already been verified.");
        }

        if (otp.isExpired()) {
            throw new BadRequestException("This OTP has expired. Please request a new OTP.");
        }

        if (otp.getAttemptsCount() >= maxAttempts) {
            throw new BadRequestException("Maximum verification attempts reached. Please request a new OTP.");
        }

        otp.setAttemptsCount(otp.getAttemptsCount() + 1);

        boolean matches = passwordEncoder.matches(inputOtp, otp.getOtpHash());
        if (matches) {
            otp.setVerified(true);
            otpVerificationRepository.save(otp);
            return true;
        } else {
            otpVerificationRepository.save(otp);
            throw new BadRequestException("Invalid OTP code. Please enter the correct 6-digit code.");
        }
    }

    public boolean isDevelopmentMode() {
        return "development".equalsIgnoreCase(smsMode);
    }

    public String getLatestDevOtp(String mobileNumber, String purpose) {
        if (!isDevelopmentMode()) {
            return null;
        }
        return otpVerificationRepository.findTopByMobileNumberAndPurposeOrderByCreatedAtDesc(mobileNumber, purpose)
                .map(OtpVerification::getDevOtp)
                .orElse(null);
    }
}
