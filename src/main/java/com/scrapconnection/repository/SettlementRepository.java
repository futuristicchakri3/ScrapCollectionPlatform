package com.scrapconnection.repository;

import com.scrapconnection.entity.ScrapBooking;
import com.scrapconnection.entity.Settlement;
import com.scrapconnection.entity.ShopkeeperProfile;
import com.scrapconnection.enums.SettlementMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByBooking(ScrapBooking booking);
    Optional<Settlement> findByBookingId(Long bookingId);
    boolean existsByBooking(ScrapBooking booking);
    List<Settlement> findByShopkeeperOrderBySettledAtDesc(ShopkeeperProfile shopkeeper);
    List<Settlement> findBySettlementMethod(SettlementMethod method);

    @Query("SELECT COALESCE(SUM(s.netAmount), 0) FROM Settlement s WHERE s.shopkeeper = :shopkeeper")
    BigDecimal sumSettledAmountByShopkeeper(@Param("shopkeeper") ShopkeeperProfile shopkeeper);

    @Query("SELECT COALESCE(SUM(s.netAmount), 0) FROM Settlement s")
    BigDecimal sumTotalSettledAmountPlatform();
}
