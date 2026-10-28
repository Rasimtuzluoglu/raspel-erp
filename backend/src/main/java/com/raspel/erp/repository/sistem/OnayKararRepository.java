package com.raspel.erp.repository.sistem;

import com.raspel.erp.entity.sistem.OnayKarar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Onay karar geçmişi sorguları.
 *
 * <p>Şirket kimliği zorunludur (tenant izolasyonu); diğer filtreler
 * {@code null} verildiğinde uygulanmaz. {@code q} ham terim değil,
 * {@link com.raspel.erp.util.AramaTemizleyici#like(String)} desenidir.
 */
@Repository
public interface OnayKararRepository extends JpaRepository<OnayKarar, Long> {

    @Query("SELECT k FROM OnayKarar k WHERE k.sirketId = :sirketId " +
            "AND (:tur IS NULL OR k.tur = :tur) " +
            "AND (:karar IS NULL OR k.karar = :karar) " +
            "AND (:baslangic IS NULL OR k.olusturmaTarihi >= :baslangic) " +
            "AND (:bitis IS NULL OR k.olusturmaTarihi < :bitis) " +
            "AND (:q IS NULL OR LOWER(k.tur) LIKE :q ESCAPE '\\' " +
            "     OR LOWER(COALESCE(k.kararVerenAd, '')) LIKE :q ESCAPE '\\' " +
            "     OR LOWER(COALESCE(k.gerekce, '')) LIKE :q ESCAPE '\\')")
    Page<OnayKarar> filtrele(
            @Param("sirketId") Long sirketId,
            @Param("tur") String tur,
            @Param("karar") String karar,
            @Param("q") String q,
            @Param("baslangic") LocalDateTime baslangic,
            @Param("bitis") LocalDateTime bitis,
            Pageable pageable);
}
