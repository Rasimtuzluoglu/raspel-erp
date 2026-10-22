package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.Masraf;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MasrafRepository extends JpaRepository<Masraf, Long> {
    Page<Masraf> findBySirketIdOrderByTarihDesc(Long sirketId, Pageable pageable);
    List<Masraf> findBySirketIdAndTarihBetween(Long sirketId, LocalDate baslangic, LocalDate bitis);

    /** KPI özeti: toplam masraf kayıt sayısı. */
    long countBySirketId(Long sirketId);

    /** KPI özeti: tarih aralığındaki masraf tutarı toplamı. */
    @Query("SELECT COALESCE(SUM(m.tutar), 0) FROM Masraf m " +
            "WHERE m.sirketId = :sirketId AND m.tarih BETWEEN :baslangic AND :bitis")
    BigDecimal sumTutarBySirketIdAndTarihBetween(@Param("sirketId") Long sirketId,
                                                @Param("baslangic") LocalDate baslangic,
                                                @Param("bitis") LocalDate bitis);

    /** KPI özeti: tarih aralığındaki KDV tutarı toplamı. */
    @Query("SELECT COALESCE(SUM(m.kdvTutar), 0) FROM Masraf m " +
            "WHERE m.sirketId = :sirketId AND m.tarih BETWEEN :baslangic AND :bitis")
    BigDecimal sumKdvBySirketIdAndTarihBetween(@Param("sirketId") Long sirketId,
                                               @Param("baslangic") LocalDate baslangic,
                                               @Param("bitis") LocalDate bitis);

    /**
     * Masraf arama/filtreleme: kategori, tarih aralığı ve serbest metin
     * (kategori, açıklama, belge no).
     */
    @Query("SELECT m FROM Masraf m WHERE m.sirketId = :sirketId " +
            "AND (:kategori IS NULL OR m.kategori = :kategori) " +
            "AND (:baslangic IS NULL OR m.tarih >= :baslangic) " +
            "AND (:bitis IS NULL OR m.tarih <= :bitis) " +
            "AND (:q IS NULL OR (LOWER(COALESCE(m.kategori,'')) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(COALESCE(m.aciklama,'')) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(COALESCE(m.belgeNo,'')) LIKE LOWER(CONCAT('%', :q, '%'))))")
    Page<Masraf> filtrele(
            @Param("sirketId") Long sirketId,
            @Param("kategori") String kategori,
            @Param("baslangic") LocalDate baslangic,
            @Param("bitis") LocalDate bitis,
            @Param("q") String q,
            Pageable pageable);
}
