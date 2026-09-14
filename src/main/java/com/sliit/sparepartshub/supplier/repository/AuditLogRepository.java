package com.sliit.sparepartshub.supplier.repository;

import com.sliit.sparepartshub.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {
}
