package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.StokSayim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface StokSayimRepository extends JpaRepository<StokSayim, Long> {
    Page<StokSayim> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);

    java.util.Optional<StokSayim> findFirstBySirketIdAndStokIdAndDurumOrderByOlusturmaTarihiDesc(
            Long sirketId, Long stokId, String durum);

    /** Barkod taraması için aynı güne ait TASLAK sayım. */
    java.util.Optional<StokSayim> findFirstBySirketIdAndStokIdAndDurumAndTarihOrderByOlusturmaTarihiDesc(
            Long sirketId, Long stokId, String durum, LocalDate tarih);

    long countBySirketId(Long sirketId);

    long countBySirketIdAndDurum(Long sirketId, String durum);

    long countBySirketIdAndTarih(Long sirketId, LocalDate tarih);

    /** Tamamlanmış ve uygulanan farkı sıfır olmayan sayımlar (KPI). */
    @Query("SELECT COUNT(s) FROM StokSayim s WHERE s.sirketId = :sirketId " +
           "AND s.durum = 'TAMAMLANDI' AND s.fark IS NOT NULL AND s.fark <> 0")
    long countFarkliTamamlanan(@Param("sirketId") Long sirketId);

    /**
     * Sunucu taraflı liste: ürün/stok kodu/barkod araması, durum ve tarih aralığı.
     * {@code :q} {@code AramaTemizleyici.like(q)} desenidir (joker kaçışlı).
     */
    @Query("SELECT s FROM StokSayim s JOIN s.stok st WHERE s.sirketId = :sirketId " +
           "AND (:durum IS NULL OR s.durum = :durum) " +
           "AND (:q IS NULL OR LOWER(st.ad) LIKE :q ESCAPE '\\' " +
           "  OR LOWER(st.stokKodu) LIKE :q ESCAPE '\\' " +
           "  OR LOWER(st.barkod) LIKE :q ESCAPE '\\') " +
           "AND (:baslangic IS NULL OR s.tarih >= :baslangic) " +
           "AND (:bitis IS NULL OR s.tarih <= :bitis)")
    Page<StokSayim> filtreliGetir(@Param("sirketId") Long sirketId,
                                  @Param("q") String q,
                                  @Param("durum") String durum,
                                  @Param("baslangic") LocalDate baslangic,
                                  @Param("bitis") LocalDate bitis,
                                  Pageable pageable);
}
