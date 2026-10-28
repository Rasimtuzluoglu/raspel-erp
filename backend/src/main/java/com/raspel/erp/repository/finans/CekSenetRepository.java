package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.CekSenet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CekSenetRepository extends JpaRepository<CekSenet, Long> {
    Page<CekSenet> findBySirketIdOrderByVadeTarihiAsc(Long sirketId, Pageable pageable);

    long countByCariHesapId(Long cariHesapId);

    /**
     * Sunucu taraflı çek/senet listesi.
     *
     * <p>{@code q} ham terim değil, joker kaçışlı LIKE desenidir
     * ({@link com.raspel.erp.util.AramaTemizleyici}). Legacy {@code ODENDI}
     * kayıtları {@code TAHSIL_EDILDI} filtresinde de döner. Tarih filtresi
     * {@code COALESCE} ile yazılır: null parametrelerde Postgres parametre
     * tipini çözemez (42P18).
     */
    @Query("SELECT c FROM CekSenet c WHERE c.sirketId = :sirketId " +
           "AND (:q IS NULL OR LOWER(c.cekNo) LIKE :q ESCAPE '\\' " +
           "     OR LOWER(c.bankaAdi) LIKE :q ESCAPE '\\' " +
           "     OR LOWER(c.hesapNo) LIKE :q ESCAPE '\\' " +
           "     OR LOWER(c.aciklama) LIKE :q ESCAPE '\\' " +
           "     OR EXISTS (SELECT 1 FROM CariHesap ch WHERE ch.id = c.cariHesapId " +
           "                 AND LOWER(ch.ad) LIKE :q ESCAPE '\\')) " +
           "AND (:tur IS NULL OR c.tur = :tur) " +
           "AND (:durum IS NULL OR c.durum = :durum " +
           "     OR (:durum = 'TAHSIL_EDILDI' AND c.durum = 'ODENDI')) " +
           "AND (:cariId IS NULL OR c.cariHesapId = :cariId) " +
           "AND c.vadeTarihi >= COALESCE(:bas, c.vadeTarihi) " +
           "AND c.vadeTarihi <= COALESCE(:bit, c.vadeTarihi) " +
           "AND (:minTutar IS NULL OR c.tutar >= :minTutar) " +
           "AND (:protesto IS NULL " +
           "     OR (:protesto = true AND c.durum = 'PROTESTO') " +
           "     OR (:protesto = false AND c.durum <> 'PROTESTO'))")
    Page<CekSenet> filtreliGetir(@Param("sirketId") Long sirketId,
                                 @Param("q") String q,
                                 @Param("tur") String tur,
                                 @Param("durum") String durum,
                                 @Param("cariId") Long cariId,
                                 @Param("bas") LocalDate bas,
                                 @Param("bit") LocalDate bit,
                                 @Param("minTutar") BigDecimal minTutar,
                                 @Param("protesto") Boolean protesto,
                                 Pageable pageable);

    /** KPI: durum sayımı (legacy ODENDI -> TAHSIL_EDILDI eşlemesi ile). */
    @Query("SELECT COUNT(c) FROM CekSenet c WHERE c.sirketId = :sirketId " +
           "AND (c.durum = :durum OR (:durum = 'TAHSIL_EDILDI' AND c.durum = 'ODENDI')) " +
           "AND c.vadeTarihi >= COALESCE(:bas, c.vadeTarihi) " +
           "AND c.vadeTarihi <= COALESCE(:bit, c.vadeTarihi)")
    long countDurum(@Param("sirketId") Long sirketId,
                    @Param("durum") String durum,
                    @Param("bas") LocalDate bas,
                    @Param("bit") LocalDate bit);

    /** KPI: vadesi geçmiş, hâlâ açık (portföy/teminat) kayıtlar. */
    @Query("SELECT COUNT(c) FROM CekSenet c WHERE c.sirketId = :sirketId " +
           "AND c.durum IN ('PORTFOY', 'TEMINATTA') AND c.vadeTarihi < :tarih")
    long countVadesiGecen(@Param("sirketId") Long sirketId, @Param("tarih") LocalDate tarih);

    /** KPI: verilen aralıkta vadesi gelen, hâlâ açık (portföy/teminat) kayıtlar. */
    @Query("SELECT COUNT(c) FROM CekSenet c WHERE c.sirketId = :sirketId " +
           "AND c.durum IN ('PORTFOY', 'TEMINATTA') " +
           "AND c.vadeTarihi BETWEEN :bas AND :bit")
    long countVadesiGelen(@Param("sirketId") Long sirketId,
                          @Param("bas") LocalDate bas,
                          @Param("bit") LocalDate bit);

    /** KPI: kapsamdaki tüm kayıtların tutar toplamı. */
    @Query("SELECT COALESCE(SUM(c.tutar), 0) FROM CekSenet c WHERE c.sirketId = :sirketId " +
           "AND c.vadeTarihi >= COALESCE(:bas, c.vadeTarihi) " +
           "AND c.vadeTarihi <= COALESCE(:bit, c.vadeTarihi)")
    BigDecimal toplamTutar(@Param("sirketId") Long sirketId,
                           @Param("bas") LocalDate bas,
                           @Param("bit") LocalDate bit);

    /** Vade takvimi: aralıktaki kayıtlar (vade sırasıyla). */
    List<CekSenet> findBySirketIdAndVadeTarihiBetweenOrderByVadeTarihiAsc(
            Long sirketId, LocalDate bas, LocalDate bit);
}
