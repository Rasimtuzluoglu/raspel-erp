package com.raspel.erp.repository;

import com.raspel.erp.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByTarihDesc();
    List<AuditLog> findByKullaniciIdOrderByTarihDesc(Long kullaniciId);
}
