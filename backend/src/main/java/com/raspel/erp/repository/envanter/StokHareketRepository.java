package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.StokHareket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.raspel.erp.entity.envanter.Stok;

@Repository
public interface StokHareketRepository extends JpaRepository<StokHareket, Long> {

    /** Hareket silme sırasında eşzamanlı işlemleri serileştirmek için kilitli okuma. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM StokHareket h WHERE h.id = :id")
    Optional<StokHareket> findByIdForUpdate(@Param("id") Long id);
    @EntityGraph(attributePaths = {"stok", "cariHesap"})
    List<StokHareket> findByStokIdOrderByHareketTarihiDesc(Long stokId);

    @EntityGraph(attributePaths = {"stok", "cariHesap"})
    List<StokHareket> findByStokSirketIdOrderByHareketTarihiDesc(Long sirketId);

    @Override
    @EntityGraph(attributePaths = {"stok", "cariHesap"})
    List<StokHareket> findAll();

    long countByStokId(Long stokId);
    long countByStokSirketIdAndTur(Long sirketId, String tur);

    @Query("SELECT new map(h.stok.ad as stokAd, h.stok.stokKodu as stokKodu, SUM(h.miktar) as satisMiktari) FROM StokHareket h WHERE h.tur = 'CIKIS' AND h.stok.sirketId = :sirketId GROUP BY h.stok.ad, h.stok.stokKodu ORDER BY SUM(h.miktar) DESC")
    List<Map<String, Object>> enCokSatanlar();

    @Query("SELECT new map(h.stok.ad as stokAd, h.stok.stokKodu as stokKodu, SUM(h.miktar) as satisMiktari) FROM StokHareket h WHERE h.tur = 'CIKIS' AND h.stok.sirketId = :sirketId GROUP BY h.stok.ad, h.stok.stokKodu ORDER BY SUM(h.miktar) DESC")
    List<Map<String, Object>> enCokSatanlarBySirket(Long sirketId);

    long countByCariHesap_Id(Long cariHesapId);

    // Talep tahmini icin: sirket genelinde son N gunun CIKIS toplamlari (stok bazli,
    // tek sorgu). N+1 ve sinirsiz gecmis yuklemesini ortadan kaldirir.
    @Query("SELECT new map(h.stok.id as stokId, COALESCE(SUM(h.miktar), 0) as toplam) " +
            "FROM StokHareket h WHERE h.tur = 'CIKIS' AND h.stok.sirketId = :sirketId " +
            "AND h.hareketTarihi >= :baslangic GROUP BY h.stok.id")
    List<Map<String, Object>> sonCikisToplamlari(@Param("sirketId") Long sirketId,
                                                 @Param("baslangic") LocalDate baslangic);
}
