package com.scrapconnection.repository;

import com.scrapconnection.entity.CustomerOrder;
import com.scrapconnection.entity.OrderPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {
    Optional<OrderPayment> findByOrder(CustomerOrder order);
}
