package com.scrapconnection.repository;

import com.scrapconnection.entity.Coupon;
import com.scrapconnection.entity.CouponLedger;
import com.scrapconnection.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponLedgerRepository extends JpaRepository<CouponLedger, Long> {
    List<CouponLedger> findByCustomerProfileOrderByTimestampDesc(CustomerProfile customerProfile);
    List<CouponLedger> findByCouponOrderByTimestampDesc(Coupon coupon);
}
