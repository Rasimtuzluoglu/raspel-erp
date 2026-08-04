package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.CariHesap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT COALESCE(SUM(c.bakiye), 0) FROM CariHesap c WHERE c.sirketId = :sirketId")
    BigDecimal toplamBakiyeHesaplaBySirketId(@Param("sirketId") Long sirketId);

    @Query("SELECT COALESCE(SUM(c.bakiye), 0) FROM CariHesap c WHERE c.bakiye > 0 AND c.sirketId = :sirketId")
    BigDecimal toplamPozitifBakiyeBySirketId(@Param("sirketId") Long sirketId);

    @Query("SELECT COALESCE(SUM(c.bakiye), 0) FROM CariHesap c WHERE c.bakiye < 0 AND c.sirketId = :sirketId")
    BigDecimal toplamNegatifBakiyeBySirketId(@Param("sirketId") Long sirketId);

    Page<CariHesap> findBySirketId(Long sirketId, Pageable pageable);

    List<CariHesap> findBySirketIdAndAdContainingIgnoreCase(Long sirketId, String query);

    @Query("SELECT COUNT(c) FROM CariHesap c WHERE c.sirketId = :sirketId")
    long countBySirketId(@Param("sirketId") Long sirketId);
}