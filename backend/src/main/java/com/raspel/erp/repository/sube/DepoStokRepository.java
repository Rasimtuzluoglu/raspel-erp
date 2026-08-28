package com.raspel.erp.repository.sube;

import com.raspel.erp.entity.sube.DepoStok;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepoStokRepository extends JpaRepository<DepoStok, Long> {
    List<DepoStok> findByDepoId(Long depoId);
    List<DepoStok> findByStokId(Long stokId);
    Optional<DepoStok> findByDepoIdAndStokId(Long depoId, Long stokId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ds FROM DepoStok ds WHERE ds.depoId = :depoId AND ds.stokId = :stokId")
    Optional<DepoStok> findByDepoIdAndStokIdForUpdate(@Param("depoId") Long depoId, @Param("stokId") Long stokId);

    @Query("SELECT COALESCE(SUM(ds.miktar), 0) FROM DepoStok ds WHERE ds.stokId = :stokId")
    BigDecimal toplamStokMiktari(Long stokId);
}
