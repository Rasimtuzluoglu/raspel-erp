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
    List<DepoStok> findByDepoIdIn(java.util.Collection<Long> depoIdler);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ds FROM DepoStok ds WHERE ds.depoId = :depoId AND ds.stokId = :stokId")
    Optional<DepoStok> findByDepoIdAndStokIdForUpdate(@Param("depoId") Long depoId, @Param("stokId") Long stokId);

    @Query("SELECT COALESCE(SUM(ds.miktar), 0) FROM DepoStok ds WHERE ds.stokId = :stokId")
    BigDecimal toplamStokMiktari(Long stokId);

    /**
     * KPI özeti: şirket depolarındaki toplam stok kalemi ve sıfır/negatif stoklu
     * kalem sayısı. {@code DepoStok}'ta sirket_id yok; depo üzerinden kapsamlanır.
     *
     * @return tek satır: [toplamKalem (Long), sifirKalem (Long)]
     */
    @Query("SELECT COUNT(ds), " +
           "COALESCE(SUM(CASE WHEN ds.miktar <= 0 THEN 1 ELSE 0 END), 0) " +
           "FROM DepoStok ds WHERE ds.depoId IN (SELECT d.id FROM Depo d WHERE d.sirketId = :sirketId)")
    Object[] ozet(@Param("sirketId") Long sirketId);
}
