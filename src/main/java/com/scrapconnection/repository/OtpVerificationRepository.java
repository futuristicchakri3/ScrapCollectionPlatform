package com.scrapconnection.repository;

import com.scrapconnection.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findTopByMobileNumberAndPurposeOrderByCreatedAtDesc(String mobileNumber, String purpose);
    long countByMobileNumberAndCreatedAtAfter(String mobileNumber, LocalDateTime after);
}
