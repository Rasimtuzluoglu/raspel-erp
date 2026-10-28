package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.Butce;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ButceRepository extends JpaRepository<Butce, Long> {
    Page<Butce> findBySirketIdOrderByYilDescAyDesc(Long sirketId, Pageable pageable);

    /**
     * Bütçe listesi: yıl/ay/tür/kategori ve serbest metin (ad, kategori) filtreleri.
     * {@code q} ham terim değil, joker kaçışlı LIKE desenidir.
     */
    @Query("SELECT b FROM Butce b WHERE b.sirketId = :sirketId " +
           "AND (:yil IS NULL OR b.yil = :yil) " +
           "AND (:ay IS NULL OR b.ay = :ay) " +
           "AND (:tur IS NULL OR b.tur = :tur) " +
           "AND (:durum IS NULL OR b.durum = :durum) " +
           "AND (:kategori IS NULL OR b.kategori = :kategori) " +
           "AND (:q IS NULL OR LOWER(b.ad) LIKE :q ESCAPE '\\' " +
           "  OR LOWER(b.kategori) LIKE :q ESCAPE '\\')")
    Page<Butce> filtreliGetir(@Param("sirketId") Long sirketId,
                              @Param("yil") Integer yil,
                              @Param("ay") Integer ay,
                              @Param("tur") String tur,
                              @Param("durum") String durum,
                              @Param("kategori") String kategori,
                              @Param("q") String q,
                              Pageable pageable);

    /** Filtre kutusu için yıl/tür bazlı kategori listesi. */
    @Query("SELECT DISTINCT b.kategori FROM Butce b WHERE b.sirketId = :sirketId " +
           "AND (:yil IS NULL OR b.yil = :yil) " +
           "AND (:tur IS NULL OR b.tur = :tur) " +
           "AND b.kategori IS NOT NULL AND b.kategori <> '' ORDER BY b.kategori")
    List<String> kategoriler(@Param("sirketId") Long sirketId,
                             @Param("yil") Integer yil,
                             @Param("tur") String tur);

    /** Özet: yıl ve tür bazlı bütçe toplamı (yalnızca onaylı). */
    @Query("SELECT COALESCE(SUM(b.tutar), 0) FROM Butce b WHERE b.sirketId = :sirketId " +
           "AND b.durum = 'ONAYLANDI' AND (:yil IS NULL OR b.yil = :yil) AND (:tur IS NULL OR b.tur = :tur)")
    BigDecimal toplamTutar(@Param("sirketId") Long sirketId,
                          @Param("yil") Integer yil,
                          @Param("tur") String tur);

    /** Özet: yıl ve tür bazlı kayıt sayısı (yalnızca onaylı). */
    @Query("SELECT COUNT(b) FROM Butce b WHERE b.sirketId = :sirketId " +
           "AND b.durum = 'ONAYLANDI' AND (:yil IS NULL OR b.yil = :yil) AND (:tur IS NULL OR b.tur = :tur)")
    long sayi(@Param("sirketId") Long sirketId,
              @Param("yil") Integer yil,
              @Param("tur") String tur);

    /** Onay bekleyen (taslak) bütçe sayısı. */
    @Query("SELECT COUNT(b) FROM Butce b WHERE b.sirketId = :sirketId " +
           "AND b.durum = 'TASLAK' AND (:yil IS NULL OR b.yil = :yil)")
    long countOnayBekleyen(@Param("sirketId") Long sirketId, @Param("yil") Integer yil);

    /** Onay bekleyen bütçeler. */
    @Query("SELECT b FROM Butce b WHERE b.sirketId = :sirketId AND b.durum = 'TASLAK' " +
           "AND (:yil IS NULL OR b.yil = :yil) ORDER BY b.yil DESC, b.ay DESC, b.kategori ASC")
    List<Butce> onayBekleyenler(@Param("sirketId") Long sirketId, @Param("yil") Integer yil);

    /** Matris: yıl/tür bazlı onaylı bütçe satırları. */
    @Query("SELECT b FROM Butce b WHERE b.sirketId = :sirketId AND b.yil = :yil " +
           "AND b.durum = 'ONAYLANDI' AND (:tur IS NULL OR b.tur = :tur) " +
           "ORDER BY b.kategori ASC, b.ay ASC")
    List<Butce> matrisSatirlari(@Param("sirketId") Long sirketId,
                                @Param("yil") Integer yil,
                                @Param("tur") String tur);

    /** Matris/kopyalama: yıl ve tür bazlı tüm bütçe satırları. */
    List<Butce> findBySirketIdAndYilOrderByKategoriAscAyAsc(Long sirketId, Integer yil);

    List<Butce> findBySirketIdAndYilAndTurOrderByKategoriAscAyAsc(Long sirketId, Integer yil, String tur);

    // ---------- Kategori registry entegrasyonu ----------

    long countByKategoriId(Long kategoriId);

    @Query("SELECT b.kategoriId, COUNT(b) FROM Butce b "
            + "WHERE b.sirketId = :sirketId AND b.kategoriId IS NOT NULL GROUP BY b.kategoriId")
    List<Object[]> kategoriKullanimSayilari(@Param("sirketId") Long sirketId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Butce b SET b.kategori = :ad WHERE b.kategoriId = :kategoriId")
    int kategoriAdGuncelle(@Param("kategoriId") Long kategoriId, @Param("ad") String ad);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Butce b SET b.kategoriId = :hedefId, b.kategori = :hedefAd "
            + "WHERE b.kategoriId = :kaynakId")
    int kategoriBirlestir(@Param("kaynakId") Long kaynakId,
                          @Param("hedefId") Long hedefId,
                          @Param("hedefAd") String hedefAd);
}
