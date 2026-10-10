package com.scrapconnection.service;

import com.scrapconnection.entity.CustomerProfile;
import com.scrapconnection.entity.Coupon;
import com.scrapconnection.entity.CouponLedger;
import com.scrapconnection.entity.Settlement;
import com.scrapconnection.entity.ShopkeeperProfile;
import com.scrapconnection.enums.CouponStatus;
import com.scrapconnection.enums.CouponTransactionType;
import com.scrapconnection.exception.BadRequestException;
import com.scrapconnection.exception.InsufficientBalanceException;
import com.scrapconnection.repository.CouponLedgerRepository;
import com.scrapconnection.repository.CouponRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponLedgerRepository couponLedgerRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.coupon.validity-days:90}")
    private int couponValidityDays;

    public CouponService(CouponRepository couponRepository, CouponLedgerRepository couponLedgerRepository) {
        this.couponRepository = couponRepository;
        this.couponLedgerRepository = couponLedgerRepository;
    }

    @Transactional
    public Coupon issueCouponForSettlement(Settlement settlement, CustomerProfile customer, ShopkeeperProfile shopkeeper, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Coupon amount must be greater than zero.");
        }

        String couponCode = generateUniqueCouponCode();
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(couponValidityDays);

        Coupon coupon = new Coupon(couponCode, customer, shopkeeper, settlement, amount, expiryDate);
        Coupon savedCoupon = couponRepository.save(coupon);

        CouponLedger ledger = new CouponLedger(
                savedCoupon,
                customer,
                CouponTransactionType.ISSUED,
                amount,
                BigDecimal.ZERO,
                amount,
                null,
                "Issued for scrap settlement " + (settlement != null ? "#" + settlement.getId() : "(Direct)") + " by shop " + shopkeeper.getShopName()
        );
        couponLedgerRepository.save(ledger);

        return savedCoupon;
    }

    @Transactional(readOnly = true)
    public BigDecimal getAvailableBalance(CustomerProfile customer) {
        BigDecimal balance = couponRepository.sumAvailableBalanceByCustomer(customer);
        return balance != null ? balance.setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<Coupon> getCustomerCoupons(CustomerProfile customer) {
        return couponRepository.findByCustomerProfileOrderByIssuedAtDesc(customer);
    }

    @Transactional(readOnly = true)
    public List<CouponLedger> getCustomerLedger(CustomerProfile customer) {
        return couponLedgerRepository.findByCustomerProfileOrderByTimestampDesc(customer);
    }

    /**
     * Atomically redeems available coupon balance towards an order.
     * Formula:
     * Coupon used = Minimum(orderPayable, availableCouponBalance)
     * Remaining payable = orderPayable - couponUsed
     * New coupon balance = oldCouponBalance - couponUsed
     */
    @Transactional
    public BigDecimal redeemCouponBalance(CustomerProfile customer, BigDecimal orderPayable, Long orderId) {
        if (orderPayable == null || orderPayable.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalAvailable = getAvailableBalance(customer);
        if (totalAvailable.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal toRedeem = orderPayable.min(totalAvailable);
        BigDecimal remainingToDeduct = toRedeem;

        // Fetch active/partially redeemed coupons in FIFO order (issued earliest first)
        List<Coupon> activeCoupons = couponRepository.findByCustomerProfileAndRemainingBalanceGreaterThanAndStatusInOrderByIssuedAtAsc(
                customer,
                BigDecimal.ZERO,
                Arrays.asList(CouponStatus.ACTIVE, CouponStatus.PARTIALLY_REDEEMED)
        );

        for (Coupon coupon : activeCoupons) {
            if (remainingToDeduct.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal currentBalance = coupon.getRemainingBalance();
            BigDecimal deductionForThisCoupon = currentBalance.min(remainingToDeduct);
            BigDecimal newBalance = currentBalance.subtract(deductionForThisCoupon);

            coupon.setRemainingBalance(newBalance);
            if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
                coupon.setStatus(CouponStatus.EXHAUSTED);
            } else {
                coupon.setStatus(CouponStatus.PARTIALLY_REDEEMED);
            }
            couponRepository.save(coupon);

            CouponLedger ledger = new CouponLedger(
                    coupon,
                    customer,
                    CouponTransactionType.REDEEMED,
                    deductionForThisCoupon,
                    currentBalance,
                    newBalance,
                    orderId,
                    "Redeemed for marketplace order #" + orderId
            );
            couponLedgerRepository.save(ledger);

            remainingToDeduct = remainingToDeduct.subtract(deductionForThisCoupon);
        }

        return toRedeem;
    }

    private String generateUniqueCouponCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder("CPN-");
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        String code = sb.toString();
        if (couponRepository.findByCouponCode(code).isPresent()) {
            return generateUniqueCouponCode();
        }
        return code;
    }
}
