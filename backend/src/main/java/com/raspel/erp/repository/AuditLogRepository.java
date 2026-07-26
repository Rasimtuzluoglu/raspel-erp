package com.raspel.erp.repository;

import com.raspel.erp.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByOrderByTarihDesc(Pageable pageable);
    List<AuditLog> findByKullaniciIdOrderByTarihDesc(Long kullaniciId);
}
