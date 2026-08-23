package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByKullaniciIdOrderByTarihDesc(Long kullaniciId);

    @Query(value = "SELECT DISTINCT a.islem FROM sistem.audit_log a ORDER BY a.islem", nativeQuery = true)
    List<String> findDistinctIslem();

    @Query(value = "SELECT DISTINCT a.entity_adi FROM sistem.audit_log a ORDER BY a.entity_adi", nativeQuery = true)
    List<String> findDistinctEntityAdi();

    @Query(value = "SELECT al.* FROM sistem.audit_log al WHERE " +
           "(CAST(:sirketId AS BIGINT) IS NULL OR al.sirket_id = CAST(:sirketId AS BIGINT)) AND " +
           "(CAST(:kullaniciId AS BIGINT) IS NULL OR al.kullanici_id = CAST(:kullaniciId AS BIGINT)) AND " +
           "(CAST(:islem AS VARCHAR) IS NULL OR al.islem = CAST(:islem AS VARCHAR)) AND " +
           "(CAST(:entityAdi AS VARCHAR) IS NULL OR al.entity_adi = CAST(:entityAdi AS VARCHAR)) AND " +
           "(CAST(:baslangic AS TIMESTAMP) IS NULL OR al.tarih >= CAST(:baslangic AS TIMESTAMP)) AND " +
           "(CAST(:bitis AS TIMESTAMP) IS NULL OR al.tarih <= CAST(:bitis AS TIMESTAMP)) " +
           "ORDER BY al.tarih DESC",
           nativeQuery = true)
    Page<AuditLog> filtreliGetir(@Param("sirketId") Long sirketId,
                                  @Param("kullaniciId") Long kullaniciId,
                                  @Param("islem") String islem,
                                  @Param("entityAdi") String entityAdi,
                                  @Param("baslangic") LocalDateTime baslangic,
                                  @Param("bitis") LocalDateTime bitis,
                                  Pageable pageable);
}
