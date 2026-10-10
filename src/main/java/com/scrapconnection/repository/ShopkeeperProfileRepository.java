package com.scrapconnection.repository;

import com.scrapconnection.entity.ShopkeeperProfile;
import com.scrapconnection.entity.User;
import com.scrapconnection.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopkeeperProfileRepository extends JpaRepository<ShopkeeperProfile, Long> {
    Optional<ShopkeeperProfile> findByUser(User user);
    Optional<ShopkeeperProfile> findByUserId(Long userId);
    List<ShopkeeperProfile> findByApprovalStatus(ApprovalStatus approvalStatus);
    long countByApprovalStatus(ApprovalStatus approvalStatus);
    List<ShopkeeperProfile> findByLocalityIgnoreCaseAndApprovalStatus(String locality, ApprovalStatus status);
}
