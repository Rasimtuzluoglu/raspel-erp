package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.Siparis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SiparisRepository extends JpaRepository<Siparis, Long> {
    Page<Siparis> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);
    /** Faz 0.4: DRIVER rolu yalnizca kendine atanan siparisleri gorur. */
    Page<Siparis> findBySirketIdAndDriverIdOrderByTarihDesc(Long sirketId, Long driverId, Pageable pageable);
    List<Siparis> findByCariHesapId(Long cariHesapId);
    long countByTarih(LocalDate tarih);
    long countBySirketIdAndTarih(Long sirketId, LocalDate tarih);
    long countBySirketIdAndDurumNot(Long sirketId, String durum);
    long countBySirketIdAndDurum(Long sirketId, String durum);
    long countBySirketIdAndDurumIn(Long sirketId, java.util.Collection<String> durumlar);
    long countBySirketIdAndDurumNotIn(Long sirketId, java.util.Collection<String> durumlar);

    @Query("SELECT s.siparisNo FROM Siparis s WHERE s.siparisNo LIKE :prefix% AND s.sirketId = :sirketId")
    List<String> findSiparisNoByPrefix(@Param("prefix") String prefix, @Param("sirketId") Long sirketId);

    // Siparis takip: sunucu tarafli filtre + sayfalama. ONCEDEN sabit ilk 200
    // siparis cekiliyordu; daha eski kayitlar gorunmuyor ve filtre yoktu.
    // LDESTEK: `:q` ham terim DEGIL, `AramaTemizleyici.like(q)` desenidir.
    // `geciken`: beklenen teslim tarihi gecmis siparisler.
    // `uretimde`: tamamlanmamis uretim emri bulunan siparisler.
    @Query("SELECT s FROM Siparis s WHERE s.sirketId = :sirketId " +
           "AND (:durum IS NULL OR s.durum = :durum) " +
           "AND (:sofor IS NULL OR s.driverAd = :sofor) " +
           "AND (:q IS NULL OR LOWER(s.siparisNo) LIKE :q ESCAPE '\\') " +
           "AND (:geciken = false OR EXISTS (SELECT t.id FROM Teslimat t WHERE t.siparisId = s.id " +
           "  AND t.beklenenTeslimTarihi IS NOT NULL AND t.beklenenTeslimTarihi < :bugun " +
           "  AND t.durum NOT IN ('TESLIM_EDILDI', 'IPTAL'))) " +
           "AND (:uretimde = false OR EXISTS (SELECT e.id FROM UretimEmri e WHERE e.siparisId = s.id " +
           "  AND e.durum <> 'TAMAMLANDI'))")
    Page<Siparis> filtreliGetir(@Param("sirketId") Long sirketId,
                                @Param("q") String q,
                                @Param("durum") String durum,
                                @Param("sofor") String sofor,
                                @Param("geciken") Boolean geciken,
                                @Param("uretimde") Boolean uretimde,
                                @Param("bugun") LocalDate bugun,
                                Pageable pageable);

    @Query("SELECT DISTINCT s.driverAd FROM Siparis s WHERE s.sirketId = :sirketId " +
           "AND s.driverAd IS NOT NULL AND s.driverAd <> '' ORDER BY s.driverAd")
    List<String> findDistinctDriverAd(@Param("sirketId") Long sirketId);

    long countByCariHesapId(Long cariHesapId);

    /** KPI özeti: şirketin toplam sipariş sayısı. */
    long countBySirketId(Long sirketId);
}
