package com.scrapconnection.repository;

import com.scrapconnection.entity.CustomerProfile;
import com.scrapconnection.entity.ScrapBooking;
import com.scrapconnection.entity.ShopkeeperProfile;
import com.scrapconnection.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScrapBookingRepository extends JpaRepository<ScrapBooking, Long> {

    Optional<ScrapBooking> findByBookingReference(String bookingReference);

    List<ScrapBooking> findByCustomerProfileOrderByCreatedAtDesc(CustomerProfile customerProfile);

    List<ScrapBooking> findByCustomerProfileAndStatusOrderByCreatedAtDesc(CustomerProfile customerProfile, BookingStatus status);

    List<ScrapBooking> findByAssignedShopkeeperOrderByCreatedAtDesc(ShopkeeperProfile shopkeeper);

    List<ScrapBooking> findByAssignedShopkeeperAndStatusOrderByCreatedAtDesc(ShopkeeperProfile shopkeeper, BookingStatus status);

    List<ScrapBooking> findByAssignedShopkeeperAndPreferredDateOrderByCreatedAtDesc(ShopkeeperProfile shopkeeper, LocalDate preferredDate);

    List<ScrapBooking> findByStatusOrderByCreatedAtDesc(BookingStatus status);

    List<ScrapBooking> findByStatusAndLocalityIgnoreCaseOrderByCreatedAtDesc(BookingStatus status, String locality);

    long countByStatus(BookingStatus status);

    long countByCustomerProfile(CustomerProfile customerProfile);

    long countByCustomerProfileAndStatus(CustomerProfile customerProfile, BookingStatus status);

    long countByAssignedShopkeeperAndStatus(ShopkeeperProfile shopkeeper, BookingStatus status);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM ScrapBooking b WHERE b.customerProfile = :customer AND b.status = 'COMPLETED'")
    BigDecimal sumTotalAmountByCustomerCompleted(@Param("customer") CustomerProfile customer);

    @Query("SELECT COALESCE(SUM(b.actualTotalWeight), 0) FROM ScrapBooking b WHERE b.customerProfile = :customer AND b.status = 'COMPLETED'")
    BigDecimal sumTotalWeightByCustomerCompleted(@Param("customer") CustomerProfile customer);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM ScrapBooking b WHERE b.assignedShopkeeper = :shopkeeper AND b.status = 'COMPLETED'")
    BigDecimal sumTotalAmountByShopkeeperCompleted(@Param("shopkeeper") ShopkeeperProfile shopkeeper);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM ScrapBooking b WHERE b.status = 'COMPLETED'")
    BigDecimal sumTotalAmountPlatformCompleted();
}
