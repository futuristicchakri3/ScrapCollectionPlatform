package com.scrapconnection.service;

import com.scrapconnection.entity.AdminAuditLog;
import com.scrapconnection.entity.User;
import com.scrapconnection.repository.AdminAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AdminAuditLogRepository auditLogRepository;

    public AuditService(AdminAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logAction(User adminUser, String action, String targetEntity, Long targetEntityId, String details, String ipAddress) {
        AdminAuditLog log = new AdminAuditLog(adminUser, action, targetEntity, targetEntityId, details, ipAddress);
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AdminAuditLog> getRecentLogs() {
        return auditLogRepository.findTop20ByOrderByTimestampDesc();
    }

    @Transactional(readOnly = true)
    public List<AdminAuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }
}
