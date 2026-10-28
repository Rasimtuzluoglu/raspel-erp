package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.TahsilatIslem;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TahsilatIslemRepository
        extends JpaRepository<TahsilatIslem, Long>, JpaSpecificationExecutor<TahsilatIslem> {

    /** İptal yarışını serileştirmek için satır kilidi. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM TahsilatIslem i WHERE i.id = :id")
    Optional<TahsilatIslem> findByIdForUpdate(@Param("id") Long id);

    Optional<TahsilatIslem> findByLegacyHareketId(Long legacyHareketId);

    boolean existsByLegacyHareketId(Long legacyHareketId);

    /**
     * Belirtilen ay için iptal edilmemiş tahsilat toplamı. Para birimi TRY.
     */
    @Query("SELECT COALESCE(SUM(i.tutar), 0) FROM TahsilatIslem i " +
           "WHERE i.sirketId = :sirketId AND i.durum = 'AKTIF' " +
           "AND i.tarih BETWEEN :baslangic AND :bitis")
    BigDecimal aktifTahsilatToplami(@Param("sirketId") Long sirketId,
                                    @Param("baslangic") LocalDate baslangic,
                                    @Param("bitis") LocalDate bitis);

    @Query("SELECT COUNT(i) FROM TahsilatIslem i " +
           "WHERE i.sirketId = :sirketId AND i.durum = 'AKTIF' " +
           "AND i.tarih BETWEEN :baslangic AND :bitis")
    long aktifTahsilatSayisi(@Param("sirketId") Long sirketId,
                             @Param("baslangic") LocalDate baslangic,
                             @Param("bitis") LocalDate bitis);

    /** Specification tabanlı filtreleme için sayfalı arama. */
    @Override
    Page<TahsilatIslem> findAll(Specification<TahsilatIslem> spec, Pageable pageable);
}
