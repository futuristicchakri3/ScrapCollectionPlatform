package com.scrapconnection.service;

import com.scrapconnection.entity.CustomerProfile;
import com.scrapconnection.entity.LoyaltyAccount;
import com.scrapconnection.entity.LoyaltyTransaction;
import com.scrapconnection.entity.ScrapBooking;
import com.scrapconnection.enums.BookingStatus;
import com.scrapconnection.enums.LoyaltyTransactionType;
import com.scrapconnection.exception.BadRequestException;
import com.scrapconnection.exception.ResourceNotFoundException;
import com.scrapconnection.repository.LoyaltyAccountRepository;
import com.scrapconnection.repository.LoyaltyTransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class LoyaltyService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;

    @Value("${app.loyalty.points-per-rupees:10}")
    private int rupeesPerPoint; // e.g. 1 point per 10 rupees

    public LoyaltyService(LoyaltyAccountRepository loyaltyAccountRepository,
                          LoyaltyTransactionRepository loyaltyTransactionRepository) {
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.loyaltyTransactionRepository = loyaltyTransactionRepository;
    }

    @Transactional
    public LoyaltyAccount getOrCreateAccount(CustomerProfile customerProfile) {
        return loyaltyAccountRepository.findByCustomerProfile(customerProfile)
                .orElseGet(() -> loyaltyAccountRepository.save(new LoyaltyAccount(customerProfile, 0)));
    }

    @Transactional
    public void awardPointsForBooking(ScrapBooking booking, BigDecimal scrapValue) {
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Points can only be awarded for completed scrap bookings.");
        }

        // Prevent duplicate point awards
        if (loyaltyTransactionRepository.existsByBookingAndTransactionType(booking, LoyaltyTransactionType.EARNED)) {
            return;
        }

        if (scrapValue == null || scrapValue.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        int pointsToAward = scrapValue.divide(BigDecimal.valueOf(rupeesPerPoint), 0, RoundingMode.FLOOR).intValue();
        if (pointsToAward <= 0) {
            return;
        }

        LoyaltyAccount account = getOrCreateAccount(booking.getCustomerProfile());
        account.setTotalPoints(account.getTotalPoints() + pointsToAward);
        loyaltyAccountRepository.save(account);

        LoyaltyTransaction transaction = new LoyaltyTransaction(
                account,
                booking,
                pointsToAward,
                LoyaltyTransactionType.EARNED,
                "Earned " + pointsToAward + " points for completed scrap booking " + booking.getBookingReference()
        );
        loyaltyTransactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public int getAvailablePoints(CustomerProfile customerProfile) {
        return loyaltyAccountRepository.findByCustomerProfile(customerProfile)
                .map(LoyaltyAccount::getTotalPoints)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public List<LoyaltyTransaction> getTransactionHistory(CustomerProfile customerProfile) {
        LoyaltyAccount account = getOrCreateAccount(customerProfile);
        return loyaltyTransactionRepository.findByLoyaltyAccountOrderByTimestampDesc(account);
    }
}
