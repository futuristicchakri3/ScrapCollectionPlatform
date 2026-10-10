package com.scrapconnection.repository;

import com.scrapconnection.entity.CustomerProfile;
import com.scrapconnection.entity.LoyaltyAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {
    Optional<LoyaltyAccount> findByCustomerProfile(CustomerProfile customerProfile);
    Optional<LoyaltyAccount> findByCustomerProfileId(Long customerProfileId);

    @Query("SELECT COALESCE(SUM(l.totalPoints), 0) FROM LoyaltyAccount l")
    long sumTotalPointsIssuedPlatform();
}
