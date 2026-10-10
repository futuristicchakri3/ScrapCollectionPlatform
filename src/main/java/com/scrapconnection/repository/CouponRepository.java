package com.scrapconnection.repository;

import com.scrapconnection.entity.CustomerProfile;
import com.scrapconnection.entity.Coupon;
import com.scrapconnection.entity.ShopkeeperProfile;
import com.scrapconnection.enums.CouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCouponCode(String couponCode);

    List<Coupon> findByCustomerProfileOrderByIssuedAtDesc(CustomerProfile customerProfile);

    List<Coupon> findByCustomerProfileAndStatusInOrderByIssuedAtDesc(CustomerProfile customerProfile, List<CouponStatus> statuses);

    List<Coupon> findByCustomerProfileAndRemainingBalanceGreaterThanAndStatusInOrderByIssuedAtAsc(
            CustomerProfile customerProfile, BigDecimal balanceThreshold, List<CouponStatus> statuses);

    List<Coupon> findByIssuingShopkeeperOrderByIssuedAtDesc(ShopkeeperProfile shopkeeper);

    @Query("SELECT COALESCE(SUM(c.remainingBalance), 0) FROM Coupon c WHERE c.customerProfile = :customer AND c.status IN ('ACTIVE', 'PARTIALLY_REDEEMED')")
    BigDecimal sumAvailableBalanceByCustomer(@Param("customer") CustomerProfile customer);

    @Query("SELECT COALESCE(SUM(c.originalAmount), 0) FROM Coupon c")
    BigDecimal sumTotalIssuedAmountPlatform();

    @Query("SELECT COALESCE(SUM(c.remainingBalance), 0) FROM Coupon c WHERE c.status IN ('ACTIVE', 'PARTIALLY_REDEEMED')")
    BigDecimal sumTotalActiveBalancePlatform();
}
