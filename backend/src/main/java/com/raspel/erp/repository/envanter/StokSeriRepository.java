package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.StokSeri;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StokSeriRepository extends JpaRepository<StokSeri, Long> {
    List<StokSeri> findByStokId(Long stokId);
    Page<StokSeri> findByStokSirketId(Long sirketId, Pageable pageable);

    /** Aynı stokta seri no tekilliği. */
    boolean existsByStokIdAndSeriNo(Long stokId, String seriNo);

    boolean existsByStokIdAndSeriNoAndIdNot(Long stokId, String seriNo, Long id);

    long countByStokSirketId(Long sirketId);

    long countByStokSirketIdAndDurum(Long sirketId, String durum);

    /** KPI: SKT'si bugün ile eşik arasında olan stoktaki seriler. */
    @Query("SELECT COUNT(s) FROM StokSeri s WHERE s.stok.sirketId = :sirketId " +
           "AND s.durum = 'STOKTA' AND s.sonKullanmaTarihi IS NOT NULL " +
           "AND s.sonKullanmaTarihi >= :bugun AND s.sonKullanmaTarihi <= :esik")
    long countSktYaklasan(@Param("sirketId") Long sirketId,
                          @Param("bugun") LocalDate bugun,
                          @Param("esik") LocalDate esik);

    /** KPI: SKT'si geçmiş olan stoktaki seriler. */
    @Query("SELECT COUNT(s) FROM StokSeri s WHERE s.stok.sirketId = :sirketId " +
           "AND s.durum = 'STOKTA' AND s.sonKullanmaTarihi IS NOT NULL " +
           "AND s.sonKullanmaTarihi < :bugun")
    long countSktGecmis(@Param("sirketId") Long sirketId, @Param("bugun") LocalDate bugun);

    /**
     * Sunucu taraflı liste: ürün/seri/lot araması, durum, depo ve SKT aralığı.
     * {@code :q} {@code AramaTemizleyici.like(q)} desenidir (joker kaçışlı).
     */
    @Query("SELECT s FROM StokSeri s WHERE s.stok.sirketId = :sirketId " +
           "AND (:durum IS NULL OR s.durum = :durum) " +
           "AND (:depoId IS NULL OR s.depoId = :depoId) " +
           "AND (:q IS NULL OR LOWER(s.stok.ad) LIKE :q ESCAPE '\\' " +
           "  OR LOWER(s.seriNo) LIKE :q ESCAPE '\\' " +
           "  OR LOWER(s.lotNo) LIKE :q ESCAPE '\\') " +
           "AND (:sktBaslangic IS NULL OR s.sonKullanmaTarihi >= :sktBaslangic) " +
           "AND (:sktBitis IS NULL OR s.sonKullanmaTarihi <= :sktBitis)")
    Page<StokSeri> filtreliGetir(@Param("sirketId") Long sirketId,
                                 @Param("q") String q,
                                 @Param("durum") String durum,
                                 @Param("depoId") Long depoId,
                                 @Param("sktBaslangic") LocalDate sktBaslangic,
                                 @Param("sktBitis") LocalDate sktBitis,
                                 Pageable pageable);

    @Query("SELECT s FROM StokSeri s WHERE s.stok.sirketId = :sirketId AND s.sonKullanmaTarihi IS NOT NULL " +
            "AND s.sonKullanmaTarihi <= :tarih ORDER BY s.sonKullanmaTarihi ASC")
    List<StokSeri> sonKullanmaYaklasan(@Param("sirketId") Long sirketId, @Param("tarih") LocalDate tarih);

    /** FEFO: SKT en yakin once, SKT yoksa giris sirasina gore stoktaki seriler. */
    @Query("SELECT s FROM StokSeri s WHERE s.stok.id = :stokId " +
            "AND s.durum = 'STOKTA' AND s.kalanMiktar > 0 " +
            "AND (:depoId IS NULL OR s.depoId = :depoId) " +
            "ORDER BY s.sonKullanmaTarihi ASC NULLS LAST, s.girisTarihi ASC, s.id ASC")
    List<StokSeri> fefoUygun(@Param("stokId") Long stokId, @Param("depoId") Long depoId);
}
