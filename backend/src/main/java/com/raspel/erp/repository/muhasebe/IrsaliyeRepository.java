package com.raspel.erp.repository.muhasebe;

import com.raspel.erp.entity.muhasebe.Irsaliye;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IrsaliyeRepository extends JpaRepository<Irsaliye, Long> {
    Page<Irsaliye> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);
    List<Irsaliye> findByFaturaId(Long faturaId);
    List<Irsaliye> findBySirketIdAndSiparisId(Long sirketId, Long siparisId);

    List<Irsaliye> findBySirketIdAndSiparisIdIn(Long sirketId, java.util.Collection<Long> siparisIdler);

    long countByCariHesapId(Long cariHesapId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Irsaliye i WHERE i.id = :id")
    Optional<Irsaliye> findByIdForUpdate(@Param("id") Long id);

    /**
     * Sunucu taraflı irsaliye listesi: arama (irsaliye no / cari adı), durum, tür,
     * cari, tarih aralığı ve faturalı/faturasız filtresi.
     *
     * <p>{@code q} ham terim değil, joker kaçışlı LIKE desenidir
     * ({@link com.raspel.erp.util.AramaTemizleyici}). Tarih filtresi
     * {@code COALESCE} ile yazılır: null parametrelerde Postgres parametre tipini
     * çözemez (42P18).
     */
    @Query("SELECT i FROM Irsaliye i WHERE i.sirketId = :sirketId " +
           "AND (:q IS NULL OR LOWER(i.irsaliyeNo) LIKE :q ESCAPE '\\' " +
           "     OR EXISTS (SELECT 1 FROM CariHesap c WHERE c.id = i.cariHesapId " +
           "                AND LOWER(c.ad) LIKE :q ESCAPE '\\')) " +
           "AND (:durum IS NULL OR i.durum = :durum) " +
           "AND (:tur IS NULL OR i.tur = :tur) " +
           "AND (:cariId IS NULL OR i.cariHesapId = :cariId) " +
           "AND i.tarih >= COALESCE(:bas, i.tarih) AND i.tarih <= COALESCE(:bit, i.tarih) " +
           "AND (:faturali IS NULL " +
           "     OR (:faturali = true AND i.faturaId IS NOT NULL) " +
           "     OR (:faturali = false AND i.faturaId IS NULL AND i.durum <> 'IPTAL'))")
    Page<Irsaliye> filtreliGetir(@Param("sirketId") Long sirketId,
                                 @Param("q") String q,
                                 @Param("durum") String durum,
                                 @Param("tur") String tur,
                                 @Param("cariId") Long cariId,
                                 @Param("bas") LocalDate bas,
                                 @Param("bit") LocalDate bit,
                                 @Param("faturali") Boolean faturali,
                                 Pageable pageable);

    /** Özet: durum bazlı sayım (tarih aralığı opsiyonel). */
    @Query("SELECT COUNT(i) FROM Irsaliye i WHERE i.sirketId = :sirketId AND i.durum = :durum " +
           "AND i.tarih >= COALESCE(:bas, i.tarih) AND i.tarih <= COALESCE(:bit, i.tarih)")
    long countDurum(@Param("sirketId") Long sirketId,
                    @Param("durum") String durum,
                    @Param("bas") LocalDate bas,
                    @Param("bit") LocalDate bit);

    /** Özet: verilen zaman aralığında teslim edilen irsaliye sayısı. */
    @Query("SELECT COUNT(i) FROM Irsaliye i WHERE i.sirketId = :sirketId " +
           "AND i.durum = 'TESLIM_EDILDI' AND i.teslimTarihi IS NOT NULL " +
           "AND i.teslimTarihi >= :bas AND i.teslimTarihi < :bit")
    long countTeslimEdilen(@Param("sirketId") Long sirketId,
                           @Param("bas") LocalDateTime bas,
                           @Param("bit") LocalDateTime bit);

    /** Özet: faturası olmayan, iptal edilmemiş irsaliyeler. */
    @Query("SELECT COUNT(i) FROM Irsaliye i WHERE i.sirketId = :sirketId " +
           "AND i.faturaId IS NULL AND i.durum <> 'IPTAL' " +
           "AND i.tarih >= COALESCE(:bas, i.tarih) AND i.tarih <= COALESCE(:bit, i.tarih)")
    long countFaturasiz(@Param("sirketId") Long sirketId,
                        @Param("bas") LocalDate bas,
                        @Param("bit") LocalDate bit);
}
