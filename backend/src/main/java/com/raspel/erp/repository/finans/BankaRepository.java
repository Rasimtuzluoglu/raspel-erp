package com.raspel.erp.repository.finans;

import com.raspel.erp.entity.finans.Banka;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankaRepository extends JpaRepository<Banka, Long> {
    Page<Banka> findBySirketId(Long sirketId, Pageable pageable);

    java.util.List<Banka> findBySirketIdOrderByAd(Long sirketId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(b.bakiye), 0) FROM Banka b WHERE b.sirketId = :sirketId")
    java.math.BigDecimal sumBakiyeBySirketId(@org.springframework.data.repository.query.Param("sirketId") Long sirketId);
}