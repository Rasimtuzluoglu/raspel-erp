package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.Banka;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface BankaRepository extends JpaRepository<Banka, Long> {
    Page<Banka> findBySirketId(Long sirketId, Pageable pageable);

    java.util.List<Banka> findBySirketIdOrderByAd(Long sirketId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Banka b WHERE b.id = :id")
    Optional<Banka> findByIdForUpdate(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(b.bakiye), 0) FROM Banka b WHERE b.sirketId = :sirketId")
    java.math.BigDecimal sumBakiyeBySirketId(@org.springframework.data.repository.query.Param("sirketId") Long sirketId);

    /** KPI özeti: şirketin banka hesabı sayısı. */
    long countBySirketId(Long sirketId);

    /** KPI özeti: pozitif bakiyeli hesapların toplamı. */
    @Query("SELECT COALESCE(SUM(b.bakiye), 0) FROM Banka b WHERE b.sirketId = :sirketId AND b.bakiye > 0")
    java.math.BigDecimal sumPozitifBakiyeBySirketId(@Param("sirketId") Long sirketId);

    // --- Mükerrer (IBAN / hesapNo) kontrolü: şirket içinde tekil olmalı. ---
    boolean existsBySirketIdAndIbanIgnoreCase(Long sirketId, String iban);

    boolean existsBySirketIdAndHesapNoIgnoreCase(Long sirketId, String hesapNo);

    boolean existsBySirketIdAndIbanIgnoreCaseAndIdNot(Long sirketId, String iban, Long id);

    boolean existsBySirketIdAndHesapNoIgnoreCaseAndIdNot(Long sirketId, String hesapNo, Long id);
}