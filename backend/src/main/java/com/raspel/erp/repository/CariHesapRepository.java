package com.raspel.erp.repository;

import com.raspel.erp.entity.CariHesap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

/**
 * Cari Hesap Repository
 * CariHesap entity'si için veritabanı işlemlerini yönetir.
 */
@Repository
public interface CariHesapRepository extends JpaRepository<CariHesap, Long> {
    
    /**
     * Tüm cari hesapların toplam bakiyesini hesapla
     */
    @Query("SELECT COALESCE(SUM(c.bakiye), 0) FROM CariHesap c")
    BigDecimal toplamBakiyeHesapla();

    /**
     * İsme göre cari hesapları ara (büyük/küçük harf duyarsız)
     */
    List<CariHesap> findByAdContainingIgnoreCase(String query);

    @Query("SELECT COALESCE(SUM(c.bakiye), 0) FROM CariHesap c WHERE c.bakiye > 0")
    BigDecimal toplamPozitifBakiye();

    @Query("SELECT COALESCE(SUM(c.bakiye), 0) FROM CariHesap c WHERE c.bakiye < 0")
    BigDecimal toplamNegatifBakiye();

    List<CariHesap> findBySirketId(Long sirketId);

    List<CariHesap> findBySirketIdAndAdContainingIgnoreCase(Long sirketId, String query);
}
