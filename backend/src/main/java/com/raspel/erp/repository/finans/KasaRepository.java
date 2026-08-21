package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.Kasa;
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
public interface KasaRepository extends JpaRepository<Kasa, Long> {
    Page<Kasa> findBySirketId(Long sirketId, Pageable pageable);

    java.util.List<Kasa> findBySirketIdOrderByAd(Long sirketId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT k FROM Kasa k WHERE k.id = :id")
    Optional<Kasa> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(k.bakiye), 0) FROM Kasa k WHERE k.sirketId = :sirketId")
    java.math.BigDecimal sumBakiyeBySirketId(@Param("sirketId") Long sirketId);
}