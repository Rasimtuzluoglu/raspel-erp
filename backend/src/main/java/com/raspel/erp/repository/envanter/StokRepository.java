package com.raspel.erp.repository.envanter;

import com.raspel.erp.entity.envanter.Stok;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import com.raspel.erp.entity.sistem.Not;

@Repository
public interface StokRepository extends JpaRepository<Stok, Long> {
    List<Stok> findAllByOrderByAd();
    Page<Stok> findBySirketIdOrderByAd(Long sirketId, Pageable pageable);
    List<Stok> findByAdContainingIgnoreCase(String q);
    List<Stok> findByBarkod(String barkod);
    List<Stok> findByBarkodContainingIgnoreCase(String barkod);
    List<Stok> findBySirketIdAndBarkodContainingIgnoreCase(Long sirketId, String barkod);

    @Query("SELECT SUM(s.miktar) FROM Stok s")
    BigDecimal toplamMiktar();

    @Query("SELECT s FROM Stok s WHERE s.sirketId = :sirketId AND s.minMiktar IS NOT NULL AND s.miktar <= s.minMiktar ORDER BY s.miktar ASC")
    List<Stok> kritikStoklar(Long sirketId);
}