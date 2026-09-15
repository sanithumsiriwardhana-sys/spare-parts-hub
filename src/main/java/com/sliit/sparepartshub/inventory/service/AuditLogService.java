package com.sliit.sparepartshub.inventory.service;

import com.sliit.sparepartshub.entity.AuditLog;
import com.sliit.sparepartshub.entity.User;
import com.sliit.sparepartshub.inventory.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(User user,
                       String actionType,
                       String tableName,
                       Integer recordId,
                       String beforeValue,
                       String afterValue) {

        AuditLog log = new AuditLog();

        log.setUser(user);
        log.setActionType(actionType);
        log.setTableName(tableName);
        log.setRecordId(recordId);

        // Convert plain text to valid JSON
        log.setOldValue(toJson(beforeValue));
        log.setNewValue(toJson(afterValue));

        auditLogRepository.save(log);
    }

    private String toJson(String value) {

        if (value == null) {
            return "null";
        }

        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                + "\"";
    }
}