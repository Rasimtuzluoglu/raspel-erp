package com.raspel.erp.repository.ik;

import com.raspel.erp.entity.ik.Vardiya;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VardiyaRepository extends JpaRepository<Vardiya, Long> {
    Page<Vardiya> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);
    List<Vardiya> findByPersonelIdOrderByTarihDesc(Long personelId);

    /** KPI özeti: şirketin toplam vardiya sayısı. */
    long countBySirketId(Long sirketId);

    /** KPI özeti: belirli bir tarihteki vardiya sayısı. */
    long countBySirketIdAndTarih(Long sirketId, LocalDate tarih);

    /** KPI özeti: tarih aralığındaki vardiya sayısı. */
    long countBySirketIdAndTarihBetween(Long sirketId, LocalDate baslangic, LocalDate bitis);

    /** KPI özeti: vardiyası olan farklı personel sayısı. */
    @Query("SELECT COUNT(DISTINCT v.personel.id) FROM Vardiya v WHERE v.sirketId = :sirketId")
    long countDistinctPersonelBySirketId(@Param("sirketId") Long sirketId);

    /** Aynı personel ve tarih için vardiya var mı? (çakışma engeli) */
    boolean existsByPersonelIdAndTarih(Long personelId, LocalDate tarih);

    /** Güncellemede kaydın kendisi hariç çakışma kontrolü. */
    boolean existsByPersonelIdAndTarihAndIdNot(Long personelId, LocalDate tarih, Long id);

    /**
     * Vardiya arama/filtreleme: personel, tarih aralığı, tür ve personel adı araması.
     */
    @Query("SELECT v FROM Vardiya v WHERE v.sirketId = :sirketId " +
            "AND (:personelId IS NULL OR v.personel.id = :personelId) " +
            "AND (:baslangic IS NULL OR v.tarih >= :baslangic) " +
            "AND (:bitis IS NULL OR v.tarih <= :bitis) " +
            "AND (:tur IS NULL OR v.tur = :tur) " +
            "AND (:q IS NULL OR LOWER(CONCAT(COALESCE(v.personel.ad,''), ' ', COALESCE(v.personel.soyad,''))) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Vardiya> filtrele(
            @Param("sirketId") Long sirketId,
            @Param("personelId") Long personelId,
            @Param("baslangic") LocalDate baslangic,
            @Param("bitis") LocalDate bitis,
            @Param("tur") String tur,
            @Param("q") String q,
            Pageable pageable);
}
