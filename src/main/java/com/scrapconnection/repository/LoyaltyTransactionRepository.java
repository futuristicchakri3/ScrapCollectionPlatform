package com.scrapconnection.repository;

import com.scrapconnection.entity.LoyaltyAccount;
import com.scrapconnection.entity.LoyaltyTransaction;
import com.scrapconnection.entity.ScrapBooking;
import com.scrapconnection.enums.LoyaltyTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {
    List<LoyaltyTransaction> findByLoyaltyAccountOrderByTimestampDesc(LoyaltyAccount loyaltyAccount);
    boolean existsByBookingAndTransactionType(ScrapBooking booking, LoyaltyTransactionType transactionType);
}
