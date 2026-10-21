package com.raspel.erp.repository.ik;

import com.raspel.erp.entity.ik.PersonelPuantaj;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PersonelPuantajRepository extends JpaRepository<PersonelPuantaj, Long> {
    List<PersonelPuantaj> findBySirketIdAndPersonelIdAndTarihBetweenOrderByTarihAsc(Long sirketId, Long personelId, LocalDate baslangic, LocalDate bitis);

    /** KPI özeti: şirketin toplam puantaj kayıt sayısı. */
    long countBySirketId(Long sirketId);

    /** KPI özeti: duruma göre puantaj kayıt sayısı. */
    long countBySirketIdAndDurum(Long sirketId, String durum);

    /** Aynı personel ve tarih için kayıt var mı? (çift kayıt engeli) */
    boolean existsByPersonelIdAndTarih(Long personelId, LocalDate tarih);

    /** Güncellemede kaydın kendisi hariç çift kayıt kontrolü. */
    boolean existsByPersonelIdAndTarihAndIdNot(Long personelId, LocalDate tarih, Long id);

    /**
     * Puantaj arama/filtreleme: personel, tarih aralığı, durum ve personel adı araması.
     */
    @Query("SELECT p FROM PersonelPuantaj p WHERE p.sirketId = :sirketId " +
            "AND (:personelId IS NULL OR p.personelId = :personelId) " +
            "AND (:baslangic IS NULL OR p.tarih >= :baslangic) " +
            "AND (:bitis IS NULL OR p.tarih <= :bitis) " +
            "AND (:durum IS NULL OR p.durum = :durum) " +
            "AND (:q IS NULL OR p.personelId IN (SELECT pr.id FROM Personel pr " +
            "     WHERE pr.sirketId = :sirketId " +
            "     AND LOWER(CONCAT(COALESCE(pr.ad,''), ' ', COALESCE(pr.soyad,''))) LIKE LOWER(CONCAT('%', :q, '%'))))")
    Page<PersonelPuantaj> filtrele(
            @Param("sirketId") Long sirketId,
            @Param("personelId") Long personelId,
            @Param("baslangic") LocalDate baslangic,
            @Param("bitis") LocalDate bitis,
            @Param("durum") String durum,
            @Param("q") String q,
            Pageable pageable);
}
