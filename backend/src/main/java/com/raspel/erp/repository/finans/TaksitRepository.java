package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.Taksit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaksitRepository extends JpaRepository<Taksit, Long>, JpaSpecificationExecutor<Taksit> {

    Optional<Taksit> findByIdAndSirketId(Long id, Long sirketId);

    List<Taksit> findBySirketIdAndOdemeDurumuNot(Long sirketId, String odemeDurumu);

    List<Taksit> findBySirketIdAndOdemeDurumuNotAndVadeTarihiBetweenOrderByVadeTarihiAsc(
            Long sirketId, String odemeDurumu, LocalDate baslangic, LocalDate bitis);

    List<Taksit> findBySirketIdAndVadeTarihiBetweenOrderByVadeTarihiAsc(
            Long sirketId, LocalDate baslangic, LocalDate bitis);

    List<Taksit> findBySirketIdAndCariHesapIdOrderByVadeTarihiAsc(Long sirketId, Long cariHesapId);

    /** Plan detayı: bir plana ait kalemler (taksit sırasıyla). */
    List<Taksit> findBySirketIdAndPlanNoOrderByTaksitNoAsc(Long sirketId, String planNo);

    /**
     * Plan özetleri: plan bazında cari, kurum, kalem/ödenen adet, toplam/ödenen
     * tutar ve vade aralığı. Plan sayısı düşük olduğu için sayfalamasız döner.
     */
    @Query("SELECT t.planNo, MAX(c.ad), MAX(t.kurum), COUNT(t), " +
            "SUM(CASE WHEN t.odemeDurumu = 'ODENDI' THEN 1 ELSE 0 END), " +
            "SUM(t.tutar), " +
            "SUM(CASE WHEN t.odemeDurumu = 'ODENDI' THEN t.tutar ELSE 0 END), " +
            "MIN(t.vadeTarihi), MAX(t.vadeTarihi) " +
            "FROM Taksit t JOIN t.cariHesap c " +
            "WHERE t.sirketId = :sirketId " +
            "AND (:q IS NULL OR LOWER(c.ad) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "GROUP BY t.planNo ORDER BY MIN(t.vadeTarihi) DESC")
    List<Object[]> planOzetleri(@Param("sirketId") Long sirketId, @Param("q") String q);

    void deleteByPlanNoAndSirketId(String planNo, Long sirketId);

    long countByCariHesap_Id(Long cariHesapId);

    /**
     * REDTEAM/Faz1.6: Plan toptan silinmeden önce ödenmiş kalem var mı?
     * {@code deleteByPlanNoAndSirketId} bu kontrolü atlayarak ödenmiş kalemleri
     * sessizce siliyordu.
     */
    long countByPlanNoAndSirketIdAndOdemeDurumu(String planNo, Long sirketId, String odemeDurumu);

    /** REDTEAM/Faz1.6: Tahsilat hareketine bağlı kalem sayısı (denetim izi). */
    long countByPlanNoAndSirketIdAndHareketIdIsNotNull(String planNo, Long sirketId);

    /** KPI: ödenmemiş taksitlerin toplam tutarı. */
    @Query("SELECT COALESCE(SUM(t.tutar), 0) FROM Taksit t " +
            "WHERE t.sirketId = :sirketId AND t.odemeDurumu <> 'ODENDI'")
    BigDecimal sumBekleyenTutar(@Param("sirketId") Long sirketId);

    /** KPI: ödenmemiş taksit adedi. */
    @Query("SELECT COUNT(t) FROM Taksit t WHERE t.sirketId = :sirketId AND t.odemeDurumu <> 'ODENDI'")
    long countBekleyen(@Param("sirketId") Long sirketId);

    /** KPI: vadesi geçmiş ödenmemiş taksitlerin toplam tutarı. */
    @Query("SELECT COALESCE(SUM(t.tutar), 0) FROM Taksit t " +
            "WHERE t.sirketId = :sirketId AND t.odemeDurumu <> 'ODENDI' AND t.vadeTarihi < :bugun")
    BigDecimal sumGecikmisTutar(@Param("sirketId") Long sirketId, @Param("bugun") LocalDate bugun);

    /** KPI: vadesi geçmiş ödenmemiş taksit adedi. */
    @Query("SELECT COUNT(t) FROM Taksit t " +
            "WHERE t.sirketId = :sirketId AND t.odemeDurumu <> 'ODENDI' AND t.vadeTarihi < :bugun")
    long countGecikmis(@Param("sirketId") Long sirketId, @Param("bugun") LocalDate bugun);

    /** KPI: bu ay vadesi gelen ödenmemiş taksitlerin toplam tutarı. */
    @Query("SELECT COALESCE(SUM(t.tutar), 0) FROM Taksit t " +
            "WHERE t.sirketId = :sirketId AND t.odemeDurumu <> 'ODENDI' " +
            "AND t.vadeTarihi BETWEEN :baslangic AND :bitis")
    BigDecimal sumBuAyTutar(@Param("sirketId") Long sirketId,
                            @Param("baslangic") LocalDate baslangic,
                            @Param("bitis") LocalDate bitis);

    /** KPI: bu yıl ödenen taksitlerin toplam tutarı. */
    @Query("SELECT COALESCE(SUM(t.tutar), 0) FROM Taksit t " +
            "WHERE t.sirketId = :sirketId AND t.odemeDurumu = 'ODENDI' " +
            "AND t.odemeTarihi BETWEEN :baslangic AND :bitis")
    BigDecimal sumOdenenTutar(@Param("sirketId") Long sirketId,
                              @Param("baslangic") LocalDate baslangic,
                              @Param("bitis") LocalDate bitis);

    /** KPI: bu yıl ödenen taksit adedi. */
    @Query("SELECT COUNT(t) FROM Taksit t " +
            "WHERE t.sirketId = :sirketId AND t.odemeDurumu = 'ODENDI' " +
            "AND t.odemeTarihi BETWEEN :baslangic AND :bitis")
    long countOdenen(@Param("sirketId") Long sirketId,
                     @Param("baslangic") LocalDate baslangic,
                     @Param("bitis") LocalDate bitis);
}
