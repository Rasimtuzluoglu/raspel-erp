package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.CariHesap;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Cari Hesap Repository
 * CariHesap entity'si için veritabanı işlemlerini yönetir.
 */
@Repository
public interface CariHesapRepository extends JpaRepository<CariHesap, Long> {
    
    /**
     * İsme göre cari hesapları ara (büyük/küçük harf duyarsız)
     */
    List<CariHesap> findByAdContainingIgnoreCase(String query);

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

    List<CariHesap> findBySirketIdOrderByAdAsc(Long sirketId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CariHesap c WHERE c.id = :id")
    Optional<CariHesap> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT c FROM CariHesap c WHERE c.sirketId = :sirketId " +
            "AND (:q IS NULL OR lower(c.ad) LIKE lower(concat('%', :q, '%')) OR lower(c.vergiNumarasi) LIKE lower(concat('%', :q, '%')) OR lower(c.telefon) LIKE lower(concat('%', :q, '%'))) " +
            "AND (:tur IS NULL OR c.tur = :tur OR c.tur = 'Her Ikisi') " +
            "AND (:bakiyeYonu IS NULL OR (:bakiyeYonu = 'alacak' AND c.bakiye > 0) OR (:bakiyeYonu = 'borc' AND c.bakiye < 0))")
    Page<CariHesap> filtreli(@Param("sirketId") Long sirketId,
                             @Param("q") String q,
                             @Param("tur") String tur,
                             @Param("bakiyeYonu") String bakiyeYonu,
                             Pageable pageable);
}