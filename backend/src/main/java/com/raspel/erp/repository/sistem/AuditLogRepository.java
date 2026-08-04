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
    Page<AuditLog> findAllByOrderByTarihDesc(Pageable pageable);
    List<AuditLog> findByKullaniciIdOrderByTarihDesc(Long kullaniciId);

    @Query(value = "SELECT DISTINCT a.islem FROM AuditLog a ORDER BY a.islem")
    List<String> findDistinctIslem();

    @Query(value = "SELECT DISTINCT a.entityAdi FROM AuditLog a ORDER BY a.entityAdi")
    List<String> findDistinctEntityAdi();

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:kullaniciId IS NULL OR a.kullaniciId = :kullaniciId) AND " +
           "(:islem IS NULL OR a.islem = :islem) AND " +
           "(:entityAdi IS NULL OR a.entityAdi = :entityAdi) AND " +
           "(:baslangic IS NULL OR a.tarih >= :baslangic) AND " +
           "(:bitis IS NULL OR a.tarih <= :bitis) " +
           "ORDER BY a.tarih DESC")
    Page<AuditLog> filtreliGetir(@Param("kullaniciId") Long kullaniciId,
                                  @Param("islem") String islem,
                                  @Param("entityAdi") String entityAdi,
                                  @Param("baslangic") LocalDateTime baslangic,
                                  @Param("bitis") LocalDateTime bitis,
                                  Pageable pageable);
}