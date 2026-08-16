package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.Hareket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

/**
 * Hareket Repository
 * Hareket entity'si için veritabanı işlemlerini yönetir.
 */
@Repository
public interface HareketRepository extends JpaRepository<Hareket, Long> {

    @Query("SELECT COALESCE(SUM(h.tutar), 0) FROM Hareket h WHERE h.tur = :tur AND h.hareketTarihi = :tarih AND h.sirketId = :sirketId")
    BigDecimal sumTutarByTurAndHareketTarihi(@Param("tur") Hareket.HareketTuru tur, @Param("tarih") LocalDate tarih, @Param("sirketId") Long sirketId);

    @Query(value = "SELECT TO_CHAR(h.hareket_tarihi, 'YYYY-MM') AS ay, " +
           "COALESCE(SUM(CASE WHEN h.tur = 'TAHSILAT' THEN h.tutar ELSE 0 END), 0) AS gelir, " +
           "COALESCE(SUM(CASE WHEN h.tur = 'ODEME' THEN h.tutar ELSE 0 END), 0) AS gider " +
           "FROM cari.hareket h WHERE h.hareket_tarihi >= :baslangic AND h.sirket_id = :sirketId " +
           "GROUP BY ay ORDER BY ay", nativeQuery = true)
    List<Object[]> aylikGelirGider(@Param("baslangic") LocalDate baslangic, @Param("sirketId") Long sirketId);

    @EntityGraph(attributePaths = {"cariHesap"})
    List<Hareket> findBySirketIdOrderByHareketTarihiDescOlusturmaTarihiDesc(Long sirketId, Pageable pageable);

    @EntityGraph(attributePaths = {"cariHesap"})
    List<Hareket> findByCariHesapIdOrderByHareketTarihiDesc(Long cariHesapId);

    @EntityGraph(attributePaths = {"cariHesap"})
    Page<Hareket> findBySirketIdOrderByHareketTarihiDesc(Long sirketId, Pageable pageable);

    @EntityGraph(attributePaths = {"cariHesap"})
    List<Hareket> findAllByOrderByHareketTarihiDescOlusturmaTarihiDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"cariHesap"})
    List<Hareket> findAllByOrderByHareketTarihiDesc();

    @Override
    @EntityGraph(attributePaths = {"cariHesap"})
    List<Hareket> findAll();

    long countByCariHesapId(Long cariHesapId);

    @EntityGraph(attributePaths = {"cariHesap"})
    List<Hareket> findByHareketTarihiBetweenOrderByHareketTarihiDesc(LocalDate baslangic, LocalDate bitis);

    @EntityGraph(attributePaths = {"cariHesap"})
    Page<Hareket> findByHareketTarihiBetween(LocalDate baslangic, LocalDate bitis, Pageable pageable);

    @EntityGraph(attributePaths = {"cariHesap"})
    Page<Hareket> findBySirketIdAndHareketTarihiBetween(Long sirketId, LocalDate baslangic, LocalDate bitis, Pageable pageable);

    @EntityGraph(attributePaths = {"cariHesap"})
    Page<Hareket> findBySirketIdAndCariHesapIdAndHareketTarihiBetween(Long sirketId, Long cariHesapId, LocalDate baslangic, LocalDate bitis, Pageable pageable);

    @EntityGraph(attributePaths = {"cariHesap"})
    List<Hareket> findByCariHesapIdAndHareketTarihiBetweenOrderByHareketTarihiDesc(Long cariHesapId, LocalDate baslangic, LocalDate bitis);

    @EntityGraph(attributePaths = {"cariHesap"})
    Page<Hareket> findByCariHesapIdAndHareketTarihiBetween(Long cariHesapId, LocalDate baslangic, LocalDate bitis, Pageable pageable);

    @EntityGraph(attributePaths = {"cariHesap"})
    List<Hareket> findByCariHesapIdAndHareketTarihiBetweenOrderByHareketTarihiAsc(Long cariHesapId, LocalDate baslangic, LocalDate bitis);

    @EntityGraph(attributePaths = {"cariHesap"})
    List<Hareket> findByHareketTarihiBetweenOrderByHareketTarihiAsc(LocalDate baslangic, LocalDate bitis);

    @EntityGraph(attributePaths = {"cariHesap"})
    List<Hareket> findBySirketIdAndHareketTarihiBetweenOrderByHareketTarihiAsc(Long sirketId, LocalDate baslangic, LocalDate bitis);

    @EntityGraph(attributePaths = {"cariHesap"})
    List<Hareket> findBySirketIdAndHareketTarihiBetweenOrderByHareketTarihiDesc(Long sirketId, LocalDate baslangic, LocalDate bitis);
}