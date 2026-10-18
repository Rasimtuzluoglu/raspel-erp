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

    /** Saklama süresi dolan denetim kayıtlarını siler (retention job). */
    long deleteByTarihBefore(java.time.LocalDateTime tarih);

    // Tenant-scoped: distinct degerler yalnizca ilgili sirketin kayitlarindan gelir.
    // ONCEDEN sirket filtresi YOKTU; bir sirketin kullanicisi baska sirketlerin
    // islem/entity adlarini filtre menu'sinde gorebiliyordu (capraz-tenant sizinti).
    @Query(value = "SELECT DISTINCT a.islem FROM sistem.audit_log a WHERE a.sirket_id = CAST(:sirketId AS BIGINT) ORDER BY a.islem", nativeQuery = true)
    List<String> findDistinctIslem(@Param("sirketId") Long sirketId);

    @Query(value = "SELECT DISTINCT a.entity_adi FROM sistem.audit_log a WHERE a.sirket_id = CAST(:sirketId AS BIGINT) ORDER BY a.entity_adi", nativeQuery = true)
    List<String> findDistinctEntityAdi(@Param("sirketId") Long sirketId);

    /**
     * Filtreli + sayfalı denetim kaydı sorgusu.
     *
     * <p>Native SQL: JPQL denemesinde PostgreSQL, null parametrelerin tipini
     * belirleyemiyordu ({@code could not determine data type of parameter}).
     * Bu yüzden her parametre açıkça {@code CAST} edilir. {@code q} ile
     * açıklama/entity/IP içinde metin arama, {@code entityId} ile belirli
     * kayda, {@code riskli} ile yalnızca silme/hata kayıtlarına inilir.
     */
    @Query(value = "SELECT al.* FROM sistem.audit_log al WHERE " +
           "(CAST(:sirketId AS BIGINT) IS NULL OR al.sirket_id = CAST(:sirketId AS BIGINT)) AND " +
           "(CAST(:kullaniciId AS BIGINT) IS NULL OR al.kullanici_id = CAST(:kullaniciId AS BIGINT)) AND " +
           "(CAST(:islem AS VARCHAR) IS NULL OR al.islem = CAST(:islem AS VARCHAR)) AND " +
           "(CAST(:entityAdi AS VARCHAR) IS NULL OR al.entity_adi = CAST(:entityAdi AS VARCHAR)) AND " +
           "(CAST(:entityId AS BIGINT) IS NULL OR al.entity_id = CAST(:entityId AS BIGINT)) AND " +
           "(CAST(:riskli AS BOOLEAN) = FALSE OR al.islem IN ('SIL', 'HATA')) AND " +
           "(CAST(:baslangic AS TIMESTAMP) IS NULL OR al.tarih >= CAST(:baslangic AS TIMESTAMP)) AND " +
           "(CAST(:bitis AS TIMESTAMP) IS NULL OR al.tarih <= CAST(:bitis AS TIMESTAMP)) AND " +
           "(CAST(:q AS VARCHAR) IS NULL OR (" +
           "  LOWER(COALESCE(al.aciklama, '')) LIKE CAST(:q AS VARCHAR) ESCAPE '\\' OR " +
           "  LOWER(COALESCE(al.entity_adi, '')) LIKE CAST(:q AS VARCHAR) ESCAPE '\\' OR " +
           "  LOWER(COALESCE(al.ip_adresi, '')) LIKE CAST(:q AS VARCHAR) ESCAPE '\\')) " +
           "ORDER BY al.tarih DESC",
           nativeQuery = true)
    Page<AuditLog> filtreliGetir(@Param("sirketId") Long sirketId,
                                  @Param("kullaniciId") Long kullaniciId,
                                  @Param("islem") String islem,
                                  @Param("entityAdi") String entityAdi,
                                  @Param("entityId") Long entityId,
                                  @Param("riskli") Boolean riskli,
                                  @Param("q") String q,
                                  @Param("baslangic") LocalDateTime baslangic,
                                  @Param("bitis") LocalDateTime bitis,
                                  Pageable pageable);

    /**
     * İşlem tipine göre sayım (KPI özeti). {@code islem} filtresi bilinçli
     * olarak dışarıda bırakılır ki dağılım gösterilebilsin; diğer filtreler
     * (kullanıcı, entity, tarih aralığı, arama) uygulanır.
     *
     * @return her satır: [islem (String), adet (Long)]
     */
    @Query(value = "SELECT al.islem, COUNT(*) FROM sistem.audit_log al WHERE " +
           "(CAST(:sirketId AS BIGINT) IS NULL OR al.sirket_id = CAST(:sirketId AS BIGINT)) AND " +
           "(CAST(:kullaniciId AS BIGINT) IS NULL OR al.kullanici_id = CAST(:kullaniciId AS BIGINT)) AND " +
           "(CAST(:entityAdi AS VARCHAR) IS NULL OR al.entity_adi = CAST(:entityAdi AS VARCHAR)) AND " +
           "(CAST(:entityId AS BIGINT) IS NULL OR al.entity_id = CAST(:entityId AS BIGINT)) AND " +
           "(CAST(:baslangic AS TIMESTAMP) IS NULL OR al.tarih >= CAST(:baslangic AS TIMESTAMP)) AND " +
           "(CAST(:bitis AS TIMESTAMP) IS NULL OR al.tarih <= CAST(:bitis AS TIMESTAMP)) AND " +
           "(CAST(:q AS VARCHAR) IS NULL OR (" +
           "  LOWER(COALESCE(al.aciklama, '')) LIKE CAST(:q AS VARCHAR) ESCAPE '\\' OR " +
           "  LOWER(COALESCE(al.entity_adi, '')) LIKE CAST(:q AS VARCHAR) ESCAPE '\\' OR " +
           "  LOWER(COALESCE(al.ip_adresi, '')) LIKE CAST(:q AS VARCHAR) ESCAPE '\\')) " +
           "GROUP BY al.islem",
           nativeQuery = true)
    List<Object[]> ozetIslem(@Param("sirketId") Long sirketId,
                             @Param("kullaniciId") Long kullaniciId,
                             @Param("entityAdi") String entityAdi,
                             @Param("entityId") Long entityId,
                             @Param("q") String q,
                             @Param("baslangic") LocalDateTime baslangic,
                             @Param("bitis") LocalDateTime bitis);
}
