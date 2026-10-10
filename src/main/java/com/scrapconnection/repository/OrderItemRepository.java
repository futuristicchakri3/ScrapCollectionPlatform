package com.scrapconnection.repository;

import com.scrapconnection.entity.CustomerOrder;
import com.scrapconnection.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrder(CustomerOrder order);
}
