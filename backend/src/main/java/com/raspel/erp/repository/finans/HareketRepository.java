package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.Hareket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT COALESCE(SUM(h.tutar), 0) FROM Hareket h WHERE h.tur = :tur AND h.hareketTarihi = :tarih")
    BigDecimal sumTutarByTurAndHareketTarihi(@Param("tur") Hareket.HareketTuru tur, @Param("tarih") LocalDate tarih);

    @Query(value = "SELECT TO_CHAR(h.hareket_tarihi, 'YYYY-MM') AS ay, " +
           "COALESCE(SUM(CASE WHEN h.tur = 'TAHSILAT' THEN h.tutar ELSE 0 END), 0) AS gelir, " +
           "COALESCE(SUM(CASE WHEN h.tur = 'ODEME' THEN h.tutar ELSE 0 END), 0) AS gider " +
           "FROM cari.hareket h WHERE h.hareket_tarihi >= :baslangic " +
           "GROUP BY ay ORDER BY ay", nativeQuery = true)
    List<Object[]> aylikGelirGider(@Param("baslangic") LocalDate baslangic);
    
    /**
     * Belirli bir cari hesaba ait hareketleri getir
     */
    List<Hareket> findByCariHesapIdOrderByHareketTarihiDesc(Long cariHesapId);

    Page<Hareket> findBySirketIdOrderByHareketTarihiDesc(Long sirketId, Pageable pageable);
    
    /**
     * Son n hareketi getir
     */
    List<Hareket> findAllByOrderByHareketTarihiDescOlusturmaTarihiDesc(Pageable pageable);

    /**
     * Tüm hareketleri tarihe göre sıralı getir
     */
    List<Hareket> findAllByOrderByHareketTarihiDesc();

    /**
     * Belirli bir cari hesaba ait hareket sayısı
     */
    long countByCariHesapId(Long cariHesapId);

    /**
     * Tarih aralığına göre hareketleri getir
     */
    List<Hareket> findByHareketTarihiBetweenOrderByHareketTarihiDesc(LocalDate baslangic, LocalDate bitis);

    /**
     * Cari hesap ve tarih aralığına göre hareketleri getir
     */
    List<Hareket> findByCariHesapIdAndHareketTarihiBetweenOrderByHareketTarihiDesc(Long cariHesapId, LocalDate baslangic, LocalDate bitis);
    List<Hareket> findByCariHesapIdAndHareketTarihiBetweenOrderByHareketTarihiAsc(Long cariHesapId, LocalDate baslangic, LocalDate bitis);
    List<Hareket> findByHareketTarihiBetweenOrderByHareketTarihiAsc(LocalDate baslangic, LocalDate bitis);
}