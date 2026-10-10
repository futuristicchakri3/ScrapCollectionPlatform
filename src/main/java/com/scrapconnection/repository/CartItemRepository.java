package com.scrapconnection.repository;

import com.scrapconnection.entity.CartItem;
import com.scrapconnection.entity.CustomerProfile;
import com.scrapconnection.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCustomerProfileOrderByAddedAtDesc(CustomerProfile customerProfile);
    Optional<CartItem> findByCustomerProfileAndProduct(CustomerProfile customerProfile, Product product);
    void deleteByCustomerProfile(CustomerProfile customerProfile);
    long countByCustomerProfile(CustomerProfile customerProfile);
}
