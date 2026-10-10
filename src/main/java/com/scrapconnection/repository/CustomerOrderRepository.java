package com.scrapconnection.repository;

import com.scrapconnection.entity.CustomerOrder;
import com.scrapconnection.entity.CustomerProfile;
import com.scrapconnection.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    List<CustomerOrder> findByCustomerProfileOrderByCreatedAtDesc(CustomerProfile customerProfile);

    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    long countByOrderStatus(OrderStatus orderStatus);

    @Query("SELECT COALESCE(SUM(o.payableAmount), 0) FROM CustomerOrder o WHERE o.orderStatus != 'CANCELLED'")
    BigDecimal sumTotalPaidOrdersPlatform();

    @Query("SELECT COALESCE(SUM(o.couponDiscount), 0) FROM CustomerOrder o WHERE o.orderStatus != 'CANCELLED'")
    BigDecimal sumTotalCouponDiscountPlatform();
}
