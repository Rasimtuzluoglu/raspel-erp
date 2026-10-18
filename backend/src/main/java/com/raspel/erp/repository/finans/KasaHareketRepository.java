package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.KasaHareket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface KasaHareketRepository extends JpaRepository<KasaHareket, Long> {
    List<KasaHareket> findByKasaIdOrderByHareketTarihiDesc(Long kasaId);
    long countByKasaId(Long kasaId);
    List<KasaHareket> findByFaturaId(Long faturaId);
    List<KasaHareket> findByKaynakTipAndKaynakId(String kaynakTip, Long kaynakId);

    /** KPI özeti: şirketin belirli gün + türdeki kasa hareketleri toplamı. */
    @Query("SELECT COALESCE(SUM(kh.tutar), 0) FROM KasaHareket kh " +
           "WHERE kh.kasa.sirketId = :sirketId AND kh.tur = :tur AND kh.hareketTarihi = :tarih")
    BigDecimal sumBySirketVeTurVeTarih(@Param("sirketId") Long sirketId,
                                       @Param("tur") String tur,
                                       @Param("tarih") LocalDate tarih);
}