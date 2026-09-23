package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.Fatura;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.raspel.erp.entity.finans.CariHesap;

@Repository
public interface FaturaRepository extends JpaRepository<Fatura, Long> {
    @EntityGraph(attributePaths = {"cariHesap"})
    Page<Fatura> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);

    @EntityGraph(attributePaths = {"cariHesap"})
    @Query("SELECT f FROM Fatura f LEFT JOIN f.cariHesap c WHERE f.sirketId = :sirketId " +
            "AND (:q IS NULL OR lower(f.faturaNumarasi) LIKE :q OR lower(c.ad) LIKE :q) " +
            "ORDER BY f.tarih DESC")
    Page<Fatura> ara(@Param("sirketId") Long sirketId, @Param("q") String q, Pageable pageable);

    @EntityGraph(attributePaths = {"cariHesap"})
    List<Fatura> findBySirketIdOrderByTarihDesc(Long sirketId);

    /** Benzersiz fatura numarasından erişim (tenant bazlı). */
    Optional<Fatura> findFirstBySirketIdAndFaturaNumarasiIgnoreCase(Long sirketId, String faturaNumarasi);

    /**
     * Yalnızca fatura başlıklarının gerektiği raporlar için (KDV, BA-BS, nakit akışı):
     * kalemler lazy bırakılır, cari hesap eager yüklenir. N+1 ve gereksiz kalem yükü önlenir.
     */
    @EntityGraph(attributePaths = {"cariHesap"})
    @Query("SELECT f FROM Fatura f WHERE f.sirketId = :sirketId ORDER BY f.tarih DESC")
    List<Fatura> basliklariGetir(@Param("sirketId") Long sirketId);

    /** Yalnızca belirtilen tarih aralığındaki fatura başlıkları (kalemler lazy). */
    @EntityGraph(attributePaths = {"cariHesap"})
    @Query("SELECT f FROM Fatura f WHERE f.sirketId = :sirketId AND f.tarih BETWEEN :baslangic AND :bitis ORDER BY f.tarih DESC")
    List<Fatura> basliklariTarihAraligindaGetir(@Param("sirketId") Long sirketId,
                                                @Param("baslangic") java.time.LocalDate baslangic,
                                                @Param("bitis") java.time.LocalDate bitis);

    @EntityGraph(attributePaths = {"cariHesap"})
    List<Fatura> findBySirketIdAndTarihBetween(Long sirketId, java.time.LocalDate baslangic, java.time.LocalDate bitis);

    @EntityGraph(attributePaths = {"cariHesap"})
    Optional<Fatura> findTopByCariHesapIdAndSirketIdOrderByTarihDescIdDesc(Long cariHesapId, Long sirketId);

    @Override
    @EntityGraph(attributePaths = {"cariHesap"})
    List<Fatura> findAll();

    long count();
    long countBySirketId(Long sirketId);
    long countBySirketIdAndDurum(Long sirketId, Fatura.FaturaDurum durum);

    long countByCariHesapId(Long cariHesapId);

    @EntityGraph(attributePaths = {"cariHesap"})
    Page<Fatura> findByCariHesapIdAndSirketIdOrderByTarihDesc(Long cariHesapId, Long sirketId, Pageable pageable);

    /** Cari ekstre icin: kesilmis faturalar (tarih araliginda). */
    @EntityGraph(attributePaths = {"cariHesap"})
    List<Fatura> findByCariHesapIdAndDurumAndTarihBetweenOrderByTarihAscIdAsc(
            Long cariHesapId, Fatura.FaturaDurum durum, java.time.LocalDate baslangic, java.time.LocalDate bitis);

    @Query("SELECT f.faturaNumarasi FROM Fatura f WHERE f.faturaNumarasi LIKE :prefix% AND f.sirketId = :sirketId")
    List<String> findFaturaNumarasiByPrefix(@Param("prefix") String prefix, @Param("sirketId") Long sirketId);

    @EntityGraph(attributePaths = {"cariHesap"})
    List<Fatura> findByTurAndOdemeDurumuNotIn(Fatura.FaturaTur tur, java.util.List<String> odemeDurumlari);

    @EntityGraph(attributePaths = {"cariHesap"})
    List<Fatura> findBySirketIdAndDurumNotAndOdemeDurumuNotIn(Long sirketId, Fatura.FaturaDurum durum, java.util.List<String> odemeDurumlari);

    /**
     * Vadesi geçmiş faturaların kalan tutar toplamı (yalnızca toplam gerektiğinde;
     * entity listesi yüklenmez, bellek/OOM riski oluşmaz).
     */
    @Query("SELECT COALESCE(SUM(f.kalanTutar), 0) FROM Fatura f WHERE f.sirketId = :sirketId " +
            "AND f.durum = :durum AND f.odemeDurumu NOT IN :odemeDurumlari " +
            "AND f.kalanTutar > 0 AND f.vadeTarihi < :bugun")
    java.math.BigDecimal toplamVadesiGecenKalan(@Param("sirketId") Long sirketId,
                                                @Param("durum") Fatura.FaturaDurum durum,
                                                @Param("odemeDurumlari") java.util.List<String> odemeDurumlari,
                                                @Param("bugun") java.time.LocalDate bugun);

    /**
     * Belirtilen vade aralığındaki faturaların kalan tutar toplamı (aggregate; liste yüklenmez).
     */
    @Query("SELECT COALESCE(SUM(f.kalanTutar), 0) FROM Fatura f WHERE f.sirketId = :sirketId " +
            "AND f.durum = :durum AND f.odemeDurumu NOT IN :odemeDurumlari " +
            "AND f.kalanTutar > 0 AND f.vadeTarihi BETWEEN :baslangic AND :bitis")
    java.math.BigDecimal toplamKalanVadeAraliginda(@Param("sirketId") Long sirketId,
                                                   @Param("durum") Fatura.FaturaDurum durum,
                                                   @Param("odemeDurumlari") java.util.List<String> odemeDurumlari,
                                                   @Param("baslangic") java.time.LocalDate baslangic,
                                                   @Param("bitis") java.time.LocalDate bitis);

    /**
     * Vadesi yaklaşan (bugün + ileriye dönük) ve kalan tutarı olan faturalar.
     */
    @Query("SELECT f FROM Fatura f WHERE f.sirketId = :sirketId AND f.durum = :durum " +
            "AND f.odemeDurumu NOT IN :odemeDurumlari AND f.kalanTutar > 0 " +
            "AND f.vadeTarihi BETWEEN :baslangic AND :bitis " +
            "ORDER BY f.vadeTarihi ASC")
    @EntityGraph(attributePaths = {"cariHesap"})
    List<Fatura> findVadesiYaklasan(@Param("sirketId") Long sirketId,
                                     @Param("durum") Fatura.FaturaDurum durum,
                                     @Param("odemeDurumlari") java.util.List<String> odemeDurumlari,
                                     @Param("baslangic") java.time.LocalDate baslangic,
                                     @Param("bitis") java.time.LocalDate bitis,
                                     Pageable pageable);

    /**
     * Vadesi geçmiş ve kalan tutarı olan faturalar.
     */
    @Query("SELECT f FROM Fatura f WHERE f.sirketId = :sirketId AND f.durum = :durum " +
            "AND f.odemeDurumu NOT IN :odemeDurumlari AND f.kalanTutar > 0 " +
            "AND f.vadeTarihi < :bugun " +
            "ORDER BY f.vadeTarihi ASC")
    @EntityGraph(attributePaths = {"cariHesap"})
    List<Fatura> findVadesiGecen(@Param("sirketId") Long sirketId,
                                  @Param("durum") Fatura.FaturaDurum durum,
                                  @Param("odemeDurumlari") java.util.List<String> odemeDurumlari,
                                  @Param("bugun") java.time.LocalDate bugun,
                                  Pageable pageable);

    /** Vadesi gecen fatura sayisi (tam kayit yuklemeden). */
    @Query("SELECT COUNT(f) FROM Fatura f WHERE f.sirketId = :sirketId AND f.durum = :durum " +
            "AND f.odemeDurumu NOT IN :odemeDurumlari AND f.kalanTutar > 0 AND f.vadeTarihi < :bugun")
    long countVadesiGecen(@Param("sirketId") Long sirketId,
                          @Param("durum") Fatura.FaturaDurum durum,
                          @Param("odemeDurumlari") java.util.List<String> odemeDurumlari,
                          @Param("bugun") java.time.LocalDate bugun);

    /**
     * Tahsilat merkezi için ödenmemiş (kalan tutarı olan) faturalar.
     */
    @Query("SELECT f FROM Fatura f WHERE f.sirketId = :sirketId AND f.tur = :tur " +
            "AND f.durum = :durum AND f.odemeDurumu NOT IN :odemeDurumlari AND f.kalanTutar > 0 " +
            "ORDER BY f.vadeTarihi ASC")
    @EntityGraph(attributePaths = {"cariHesap"})
    List<Fatura> findTahsilatEdilecek(@Param("sirketId") Long sirketId,
                                      @Param("tur") Fatura.FaturaTur tur,
                                      @Param("durum") Fatura.FaturaDurum durum,
                                      @Param("odemeDurumlari") java.util.List<String> odemeDurumlari);

    /**
     * Tek bir cari için ödenmemiş (kalan tutarı olan) faturalar. Tahsilat tahsisi ve
     * hatırlatma akışlarında tüm şirket faturalarının belleğe yüklenmesini önler.
     */
    @Query("SELECT f FROM Fatura f WHERE f.sirketId = :sirketId AND f.cariHesap.id = :cariHesapId " +
            "AND f.tur = :tur AND f.durum = :durum AND f.odemeDurumu NOT IN :odemeDurumlari " +
            "AND f.kalanTutar > 0 ORDER BY f.vadeTarihi ASC")
    @EntityGraph(attributePaths = {"cariHesap"})
    List<Fatura> findTahsilatEdilecekByCari(@Param("sirketId") Long sirketId,
                                            @Param("cariHesapId") Long cariHesapId,
                                            @Param("tur") Fatura.FaturaTur tur,
                                            @Param("durum") Fatura.FaturaDurum durum,
                                            @Param("odemeDurumlari") java.util.List<String> odemeDurumlari);

    /**
     * Cari bazında en çok geciken fatura günü (yaşlandırma raporu). Tüm fatura listesi
     * yüklenmeden DB'de grup bazında hesaplanır: [cariHesapId, maksGecikmeGun].
     */
    @Query(value = "SELECT f.cari_hesap_id, MAX(:bugun - f.vade_tarihi) FROM fatura.fatura f " +
            "WHERE f.sirket_id = :sirketId AND f.tur = :tur AND f.durum = :durum " +
            "AND f.odeme_durumu NOT IN (:odemeDurumlari) AND f.kalan_tutar > 0 " +
            "AND f.cari_hesap_id IS NOT NULL AND f.vade_tarihi < :bugun " +
            "GROUP BY f.cari_hesap_id",
            nativeQuery = true)
    List<Object[]> cariBazindaMaksGecikme(@Param("sirketId") Long sirketId,
                                          @Param("tur") String tur,
                                          @Param("durum") String durum,
                                          @Param("odemeDurumlari") java.util.List<String> odemeDurumlari,
                                          @Param("bugun") java.time.LocalDate bugun);

    // Churn analizi icin cari bazli fatura ozeti (tam tabloyu belleğe yuklemeden).
    @Query("SELECT new map(f.cariHesap.id as cariId, MAX(f.tarih) as sonTarih, COUNT(f) as adet, " +
            "COALESCE(SUM(f.genelToplam), 0) as ciro) " +
            "FROM Fatura f WHERE f.sirketId = :sirketId AND f.cariHesap IS NOT NULL GROUP BY f.cariHesap.id")
    List<Map<String, Object>> cariFaturaOzeti(@Param("sirketId") Long sirketId);

    // Anomali: ayni cari + ayni tutarda birden fazla fatura (SQL tarafinda gruplama).
    @Query("SELECT new map(f.cariHesap.id as cariId, f.cariHesap.ad as cariAd, f.genelToplam as tutar, " +
            "COUNT(f) as adet, MIN(f.id) as ilkId) " +
            "FROM Fatura f WHERE f.sirketId = :sirketId AND f.cariHesap IS NOT NULL AND f.genelToplam IS NOT NULL " +
            "GROUP BY f.cariHesap.id, f.cariHesap.ad, f.genelToplam HAVING COUNT(f) > 1")
    List<Map<String, Object>> mukerrerFaturaGruplari(@Param("sirketId") Long sirketId);

    // Anomali: esik ustu tutarli faturalar (yalnizca eslesenler yuklenir).
    @Query("SELECT f FROM Fatura f WHERE f.sirketId = :sirketId AND f.genelToplam > :esik ORDER BY f.genelToplam DESC")
    List<Fatura> yuksekTutarliFaturalar(@Param("sirketId") Long sirketId, @Param("esik") BigDecimal esik);

    // Konsolide ciro: kesilmis satis faturalarinin toplam tutari.
    @Query("SELECT COALESCE(SUM(f.genelToplam), 0) FROM Fatura f WHERE f.sirketId = :sirketId " +
            "AND f.tur = com.raspel.erp.entity.ticaret.Fatura.FaturaTur.SATIS " +
            "AND f.durum = com.raspel.erp.entity.ticaret.Fatura.FaturaDurum.KESILDI")
    BigDecimal sumKesilmisSatisCiro(@Param("sirketId") Long sirketId);
}
