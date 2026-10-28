package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.KasaHareket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // ---------- Kategori registry entegrasyonu ----------

    long countByKategoriId(Long kategoriId);

    /** Kategori başına kasa hareketi sayısı (tek sorgu). */
    @Query("SELECT kh.kategori.id, COUNT(kh) FROM KasaHareket kh " +
           "WHERE kh.kasa.sirketId = :sirketId AND kh.kategori IS NOT NULL GROUP BY kh.kategori.id")
    List<Object[]> kategoriKullanimSayilari(@Param("sirketId") Long sirketId);

    /** Birleştirme: kaynağa bağlı kasa hareketleri hedef kategoriye taşınır. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE KasaHareket kh SET kh.kategori = :hedef WHERE kh.kategori.id = :kaynakId")
    int kategoriBirlestir(@Param("kaynakId") Long kaynakId, @Param("hedef") com.raspel.erp.entity.sistem.Kategori hedef);

    /** KPI özeti: şirketin belirli gün + türdeki kasa hareketleri toplamı. */
    @Query("SELECT COALESCE(SUM(kh.tutar), 0) FROM KasaHareket kh " +
           "WHERE kh.kasa.sirketId = :sirketId AND kh.tur = :tur AND kh.hareketTarihi = :tarih")
    BigDecimal sumBySirketVeTurVeTarih(@Param("sirketId") Long sirketId,
                                       @Param("tur") String tur,
                                       @Param("tarih") LocalDate tarih);
}