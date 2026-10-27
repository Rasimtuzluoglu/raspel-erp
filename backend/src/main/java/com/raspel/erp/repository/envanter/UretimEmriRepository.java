package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.UretimEmri;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UretimEmriRepository extends JpaRepository<UretimEmri, Long> {
    List<UretimEmri> findBySirketIdOrderByOlusturmaTarihiDesc(Long sirketId);
    List<UretimEmri> findBySirketIdAndSiparisId(Long sirketId, Long siparisId);

    List<UretimEmri> findBySirketIdAndSiparisIdIn(Long sirketId, java.util.Collection<Long> siparisIdler);

    /**
     * Borçlu kilitleme (SELECT ... FOR UPDATE). Üretim emrini tamamlama tek atomik
     * işlem olmalı: durum kontrolü, hammadde tüketimi ve mamul üretimi birlikte.
     * Kilit olmadan iki eşzamanlı istek ikisi de "TAMAMLANDI" kontrolünü geçip
     * hammaddeyi iki kez tüketiyor ve mamulü iki kez üretiyordu.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM UretimEmri e WHERE e.id = :id")
    Optional<UretimEmri> findByIdForUpdate(@Param("id") Long id);

    /** KPI özeti: aktif (tamamlanmamış) üretim emri bulunan sipariş sayısı. */
    @Query("SELECT COUNT(DISTINCT e.siparisId) FROM UretimEmri e " +
           "WHERE e.sirketId = :sirketId AND e.siparisId IS NOT NULL AND e.durum <> :durum")
    long countDistinctSiparisAktif(@Param("sirketId") Long sirketId, @Param("durum") String durum);

    /**
     * Üretim emirleri ekranı: sunucu taraflı arama + durum/öncelik/geciken/tarih
     * filtreleri + sayfalama. Önceden tüm emirler belleğe çekilip istemcide
     * filtreleniyordu; büyük veri setlerinde hem yavaş hem de eksikti.
     *
     * <p><b>{@code :q}</b> ham terim DEĞİL, {@code AramaTemizleyici.like(q)}
     * desenidir (joker kaçışlı). Ürün adı {@code UretimEmri} üzerinde kolon
     * olmadığı için {@code Stok} alt sorgusuyla eşleştirilir.
     *
     * <p><b>{@code geciken}</b>: planlanan bitiş geçmiş, tamamlanmamış/iptal
     * edilmemiş emirler.
     */
    @Query("SELECT e FROM UretimEmri e WHERE e.sirketId = :sirketId " +
           "AND (:durum IS NULL OR e.durum = :durum) " +
           "AND (:oncelik IS NULL OR e.oncelik = :oncelik) " +
           "AND (:hatId IS NULL OR e.hatId = :hatId) " +
           "AND (:q IS NULL OR EXISTS (SELECT s.id FROM Stok s WHERE s.id = e.urunId AND LOWER(s.ad) LIKE :q ESCAPE '\\')) " +
           "AND (:geciken = false OR (e.planlananBitis IS NOT NULL AND e.planlananBitis < :bugun " +
           "  AND e.durum NOT IN ('TAMAMLANDI', 'IPTAL'))) " +
           "AND (:buAy = false OR (e.durum = 'TAMAMLANDI' AND e.tamamlanmaTarihi IS NOT NULL " +
           "  AND e.tamamlanmaTarihi >= :ayBaslangic)) " +
           "AND (:baslangic IS NULL OR e.planlananBitis >= :baslangic) " +
           "AND (:bitis IS NULL OR e.planlananBitis <= :bitis)")
    Page<UretimEmri> filtreliGetir(@Param("sirketId") Long sirketId,
                                   @Param("q") String q,
                                   @Param("durum") String durum,
                                   @Param("oncelik") String oncelik,
                                   @Param("hatId") Long hatId,
                                   @Param("geciken") Boolean geciken,
                                   @Param("buAy") Boolean buAy,
                                   @Param("ayBaslangic") java.time.LocalDateTime ayBaslangic,
                                   @Param("baslangic") LocalDate baslangic,
                                   @Param("bitis") LocalDate bitis,
                                   @Param("bugun") LocalDate bugun,
                                   Pageable pageable);

    /** Hat doluluğu: hattaki açık (taslak + üretimde) emir sayısı. */
    @Query("SELECT COUNT(e) FROM UretimEmri e WHERE e.sirketId = :sirketId AND e.hatId = :hatId " +
           "AND e.durum IN ('TASLAK', 'URETIMDE')")
    long countAktifByHat(@Param("sirketId") Long sirketId, @Param("hatId") Long hatId);

    /** Hat doluluğu: hattaki geciken emir sayısı. */
    @Query("SELECT COUNT(e) FROM UretimEmri e WHERE e.sirketId = :sirketId AND e.hatId = :hatId " +
           "AND e.planlananBitis IS NOT NULL AND e.planlananBitis < :bugun " +
           "AND e.durum NOT IN ('TAMAMLANDI', 'IPTAL')")
    long countGecikenByHat(@Param("sirketId") Long sirketId, @Param("hatId") Long hatId,
                           @Param("bugun") LocalDate bugun);

    boolean existsBySirketIdAndHatId(Long sirketId, Long hatId);

    /** Analiz: dönem içinde tamamlanan emirler. */
    @Query("SELECT e FROM UretimEmri e WHERE e.sirketId = :sirketId AND e.durum = 'TAMAMLANDI' " +
           "AND e.tamamlanmaTarihi IS NOT NULL AND e.tamamlanmaTarihi >= :bas AND e.tamamlanmaTarihi < :bit")
    List<UretimEmri> tamamlananlarAralikta(@Param("sirketId") Long sirketId,
                                           @Param("bas") java.time.LocalDateTime bas,
                                           @Param("bit") java.time.LocalDateTime bit);

    /**
     * Takvim/Gantt: planlanan tarihleri verilen aralıkla kesişen, iptal
     * edilmemiş emirler.
     */
    @Query("SELECT e FROM UretimEmri e WHERE e.sirketId = :sirketId AND e.durum <> 'IPTAL' " +
           "AND ((e.planlananBaslangic IS NOT NULL AND e.planlananBitis IS NOT NULL " +
           "  AND e.planlananBaslangic <= :bitis AND e.planlananBitis >= :baslangic) " +
           " OR (e.planlananBaslangic IS NULL AND e.planlananBitis BETWEEN :baslangic AND :bitis) " +
           " OR (e.planlananBitis IS NULL AND e.planlananBaslangic BETWEEN :baslangic AND :bitis)) " +
           "ORDER BY e.planlananBaslangic ASC, e.id ASC")
    List<UretimEmri> takvimAralikta(@Param("sirketId") Long sirketId,
                                    @Param("baslangic") LocalDate baslangic,
                                    @Param("bitis") LocalDate bitis);
}
