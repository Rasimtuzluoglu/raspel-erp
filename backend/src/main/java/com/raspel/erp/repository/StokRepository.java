package com.raspel.erp.repository;

import com.raspel.erp.entity.Stok;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

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
}
