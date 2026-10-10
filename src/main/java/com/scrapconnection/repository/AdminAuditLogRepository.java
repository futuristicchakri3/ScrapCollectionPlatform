package com.scrapconnection.repository;

import com.scrapconnection.entity.AdminAuditLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    List<AdminAuditLog> findAllByOrderByTimestampDesc();
    List<AdminAuditLog> findTop20ByOrderByTimestampDesc();
}
