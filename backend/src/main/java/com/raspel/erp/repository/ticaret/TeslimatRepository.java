package com.raspel.erp.repository.ticaret;

import com.raspel.erp.entity.ticaret.Teslimat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TeslimatRepository extends JpaRepository<Teslimat, Long> {
    List<Teslimat> findBySirketIdAndDriverIdOrderByOlusturmaTarihiDesc(Long sirketId, Long driverId);
    List<Teslimat> findBySirketId(Long sirketId);
    long countBySirketIdAndDriverIdAndDurumIn(Long sirketId, Long driverId, List<String> durumlar);
    List<Teslimat> findByDurumInAndBeklenenTeslimTarihiBeforeAndGecikmeBildirildiFalse(List<String> durumlar, LocalDate tarih);
    List<Teslimat> findBySirketIdAndSiparisId(Long sirketId, Long siparisId);

    List<Teslimat> findBySirketIdAndSiparisIdIn(Long sirketId, java.util.Collection<Long> siparisIdler);

    List<Teslimat> findBySirketIdAndFaturaId(Long sirketId, Long faturaId);

    List<Teslimat> findByFaturaIdIn(java.util.Collection<Long> faturaIdler);

    boolean existsByFaturaId(Long faturaId);

    /** Fatura listelerinde "teslimati var" bayragi icin tek sorguda fatura id'leri. */
    @Query("SELECT t.faturaId FROM Teslimat t WHERE t.faturaId IN :faturaIdler")
    List<Long> findFaturaIdByFaturaIdIn(@Param("faturaIdler") java.util.Collection<Long> faturaIdler);

    /**
     * KPI özeti: beklenen teslim tarihi geçmiş, teslim edilmemiş/iptal edilmemiş
     * sipariş sayısı (distinct).
     */
    @Query("SELECT COUNT(DISTINCT t.siparisId) FROM Teslimat t WHERE t.sirketId = :sirketId " +
           "AND t.beklenenTeslimTarihi IS NOT NULL AND t.beklenenTeslimTarihi < :tarih " +
           "AND t.durum NOT IN ('TESLIM_EDILDI', 'IPTAL')")
    long countGecikenDistinctSiparis(@Param("sirketId") Long sirketId, @Param("tarih") LocalDate tarih);

    // --- Teslimatlar ekranı KPI özeti ---
    long countBySirketId(Long sirketId);

    long countBySirketIdAndDurum(Long sirketId, String durum);

    @Query("SELECT COUNT(t) FROM Teslimat t WHERE t.sirketId = :sirketId " +
           "AND t.beklenenTeslimTarihi IS NOT NULL AND t.beklenenTeslimTarihi < :tarih " +
           "AND t.durum NOT IN ('TESLIM_EDILDI', 'IPTAL')")
    long countGeciken(@Param("sirketId") Long sirketId, @Param("tarih") LocalDate tarih);
}
